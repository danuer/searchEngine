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


import static searchengine.services.IndexingServiceImpl.isInterrupted;

@Getter
@Setter
public class Parser extends RecursiveTask<Set<String>> {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private ConcurrentSkipListSet<String> linkList;
    private Set<String> childLinkList;
    private final String url;
    private final String rootUrl;

    public Parser(String url,
                  SiteRepository siteRepository,
                  PageRepository pageRepository,
                  LemmaRepository lemmaRepository,
                  IndexRepository indexRepository) {
        this.url = url;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.rootUrl = url;
        this.linkList = new ConcurrentSkipListSet<>();
        this.linkList.add(url);
    }

    public Parser(String url,
                  SiteRepository siteRepository,
                  PageRepository pageRepository,
                  ConcurrentSkipListSet<String> list,
                  String rootUrl,
                  LemmaRepository lemmaRepository,
                  IndexRepository indexRepository)
            throws IOException, InterruptedException {
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
        Page checkPage = pageRepository.searchByPath(url);
        if (checkPage == null) {
            parseLinks(url);
        }
        if (!childLinkList.isEmpty() && !isInterrupted) {
//            System.out.println("найдено " + childLinkList.size());
            childLinkList.forEach(link -> {
                try {
                    Parser task = new Parser(link, siteRepository, pageRepository, linkList, rootUrl, lemmaRepository, indexRepository);
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
//        System.out.println("linkList size - " + linkList.size());
        return linkList;
    }

    public static Connection connect(String url) {
        return Jsoup.connect(url)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) " +
                        "Gecko/20070725 Firefox/2.0.0.6")
                .referrer("https://www.google.com")
                .timeout(30000)
                .followRedirects(false);
    }

    private void parseLinks(String url) {
        try {
            Thread.sleep(500);
            Document doc = connect(url).get();
            Page savedPage = savePage(doc, url, rootUrl);
            PageIndexer indexer = new PageIndexer(url, rootUrl, siteRepository, pageRepository, savedPage, lemmaRepository, indexRepository);
            new Thread(indexer).start();

            Elements links = doc.select("a");
            links.forEach(element -> {
                String link = element.attr("abs:href");
                if (isTrueLink(link)) {
//                    System.out.println(link);
                    linkList.add(link);
                    childLinkList.add(link);
                }
            });
        } catch (InterruptedException | IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private Boolean isTrueLink(String link) {
//        String regexUrl = "(?<scheme>https?)://play" +
////                "(?<subDomain>[a-z0-9-]{1,63}\\.(?:[a-z0-9-]{1,63}\\.)*)?(?<domain>[a-z0-9-]{1,256})" +
//                rootUrl +
//                "[.](?<tld>[a-z0-9]+)(?::(?<port>[0-9]{1,5}))?(?<path>/.*/?)?";
//        Pattern pattern = Pattern.compile(regexUrl);
//        Matcher matcher = pattern.matcher(link);
//        while (matcher.find()) {
        if (!linkList.contains(link) &&
                link.startsWith(url) &&
                !link.contains("extlink") &&
                !link.equals(url) &&
                !link.contains(".pdf") &&
                !link.contains("#") &&
                !link.contains(".doc") &&
                !link.contains(".jpg") &&
                !link.contains(".png") &&
                !link.contains(".svg") &&
                !link.contains("?")) {
            return true;
        }
//        }
        return false;
    }

    public Page savePage(Document doc, String url, String rootUrl) {
        Page page = new Page();
        page.setCode(doc.connection().response().statusCode());
        page.setPath(url.substring(url.lastIndexOf(rootUrl)));
        page.setSite(siteRepository.findSiteByUrl(rootUrl));
        page.setContent(doc.html());
        Page savedPage = pageRepository.save(page);
        Site site = savedPage.getSite();
        site.setStatusTime(System.currentTimeMillis());
        siteRepository.save(site);
        return savedPage;
    }
}
