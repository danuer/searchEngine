package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import searchengine.model.*;
import searchengine.model.repositorys.IndexRepository;
import searchengine.model.repositorys.LemmaRepository;
import searchengine.model.repositorys.PageRepository;
import searchengine.model.repositorys.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static searchengine.services.IndexingServiceImpl.isInterrupted;
import static searchengine.services.PageIndexerServiceImpl.pageMapForIndexer;

@Getter
@Setter
public class Parser extends RecursiveTask<Set<String>> {

    private final PageIndexerService pageIndexerService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final ConcurrentSkipListSet<String> linkList;
    private Set<String> childLinkList;
    private final String url;
    private final String rootUrl;
    private Document doc;

    public Parser(PageIndexerService pageIndexerService, String url,
                  SiteRepository siteRepository,
                  PageRepository pageRepository,
                  LemmaRepository lemmaRepository,
                  IndexRepository indexRepository) {
        this.pageIndexerService = pageIndexerService;
        this.url = url;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.rootUrl = url;
        this.linkList = new ConcurrentSkipListSet<>();
        this.linkList.add(url);
    }

    public Parser(PageIndexerService pageIndexerService, String url,
                  SiteRepository siteRepository,
                  PageRepository pageRepository,
                  ConcurrentSkipListSet<String> list,
                  String rootUrl,
                  LemmaRepository lemmaRepository,
                  IndexRepository indexRepository)
            throws IOException, InterruptedException {
        this.pageIndexerService = pageIndexerService;
        this.url = url;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.rootUrl = rootUrl;
        this.linkList = list;
    }

    @Override
    protected Set<String> compute() {
        childLinkList = new HashSet<>();
        List<Parser> taskList = new ArrayList<>();
        parseLinks(url);
        if (!childLinkList.isEmpty() && !isInterrupted) {
            childLinkList.forEach(link -> {
                try {
                    Parser task = new Parser(pageIndexerService, link, siteRepository, pageRepository, linkList, rootUrl, lemmaRepository, indexRepository);
                    task.fork();
                    taskList.add(task);
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            });
            for (Parser task : taskList) {
                linkList.addAll(task.join());
            }
        }
        return linkList;
    }

    public static Connection connect(String url) {
        return Jsoup.connect(url)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) " +
                        "Gecko/20070725 Firefox/2.0.0.6")
                .referrer("https://www.google.com")
                .timeout(60000)
                .followRedirects(false);
    }

    private void parseLinks(String url) {
        try {
            Thread.sleep(1500);
            doc = connect(url).get();
            Page savedPage = savePage(doc, url, rootUrl);
            if (savedPage.getCode() == 200) {
                pageMapForIndexer.put(url, savedPage.getId());
            }
            Elements links = doc.select("a");
            links.forEach(element -> {
                String link = element.attr("abs:href");
                if (isTrueLink(link)) {
                    linkList.add(link);
                    childLinkList.add(link);
                }
            });
        } catch (InterruptedException | IOException | IllegalArgumentException e) {
            Logger.getLogger(Parser.class.getName()).info("Parsing of url: " + url +
                    " was interrupted due to: " + e.getMessage());
        }
    }

    private Boolean isTrueLink(String link) {
        if (!linkList.contains(link) &&
                link.startsWith(rootUrl) &&
                !link.contains("extlink") &&
                !link.equals(url) &&
                link.length() < 500 &&
                !link.contains(".pdf") &&
                !link.contains("#") &&
                !link.contains(".doc") &&
                !link.contains(".jpg") &&
                !link.contains(".JPG") &&
                !link.contains(".png") &&
                !link.contains(".svg") &&
                !link.contains(".zip") &&
                !link.contains(".nc") &&
                !link.contains(".xls") &&
                !link.contains(".fig") &&
                !link.contains(".jpeg") &&
                !link.contains(".JPEG") &&
                !link.contains(".jfif") &&
                !link.contains("?")) {
            return true;
        }
        return false;
    }

    public Page savePage(Document doc, String url, String rootUrl) {
        Page savedPage = new Page();
        String regex = "https?\\:\\/\\/[^\\/]+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            Page page = new Page();
            page.setCode(doc.connection().response().statusCode());
            if (url.equals(rootUrl)) {
                page.setPath(url);
            } else {
                page.setPath(url.substring(matcher.end()));
            }
            page.setSiteEntity(siteRepository.findSiteByUrl(rootUrl));
            page.setContent(doc.html());
            try {
                savedPage = pageRepository.save(page);
            } catch (Exception e) {
                Logger.getLogger(Parser.class.getName())
                        .info("Save of url: " + url
                                + " was interrupted due to: " + e.getMessage());
            }
            SiteEntity siteEntity = savedPage.getSiteEntity();
            siteEntity.setStatusTime(System.currentTimeMillis());
            siteRepository.save(siteEntity);
        }
        return savedPage;
    }
}
