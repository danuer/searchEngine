package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.PageRepository;
import searchengine.model.Site;
import searchengine.model.SiteRepository;

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
    private ConcurrentSkipListSet<String> linkList;
    private Set<String> childLinkList;
    private final String url;
    private final String rootUrl;

    public Parser(String url, SiteRepository siteRepository, PageRepository pageRepository) {
        this.url = url;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.rootUrl = url;
        this.linkList = new ConcurrentSkipListSet<>();
        this.linkList.add(url);
    }

    public Parser(String url, SiteRepository siteRepository, PageRepository pageRepository,
                  ConcurrentSkipListSet<String> list, String rootUrl)
            throws IOException, InterruptedException {
        this.url = url;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
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
                    Parser task = new Parser(link, siteRepository, pageRepository, linkList, rootUrl);
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

    private Connection connect(String url) {
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
            savePage(doc, url, rootUrl);

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

    private void savePage(Document doc, String url, String rootUrl) {
        Page page = new Page();
        page.setCode(doc.connection().response().statusCode());
        page.setPath(url.substring(url.lastIndexOf(rootUrl)));
        page.setSite(siteRepository.findSiteByUrl(rootUrl));
        page.setContent(doc.html());
        Page savedPage = pageRepository.save(page);
        Site site = savedPage.getSite();
        site.setStatusTime(System.currentTimeMillis());
        siteRepository.save(site);
    }
}
