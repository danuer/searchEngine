package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser extends RecursiveTask<Set<String>> {

    private SiteRepository siteRepository;
    private PageRepository pageRepository;

    private ConcurrentSkipListSet<String> linkList;
    private Set<String> childLinkList;
    private final String url;
    private final String rootUrl;
    public Parser(String url) {
        this.url = url;
        this.rootUrl = url.split("/.")[1];
        this.linkList = new ConcurrentSkipListSet<>();
        this.linkList.add(url);
    }

    public Parser(SiteRepository siteRepository, PageRepository pageRepository, String url, ConcurrentSkipListSet<String> list) throws IOException, InterruptedException {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.url = url;
        this.rootUrl = url.split("/.")[1];
        this.linkList = list;
    }

    @Override
    protected Set<String> compute() {

        childLinkList = new HashSet<>();
        List<Parser> taskList = new ArrayList<>();

        parseLinks(url);

        if (!childLinkList.isEmpty()) {
            System.out.println("найдено " + childLinkList.size());
            childLinkList.forEach(link -> {
                try {
                    Parser task = new Parser(siteRepository, pageRepository, link, linkList);
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
        System.out.println("linkList size - " + linkList.size());
        return linkList;
    }

    private Connection connect(String url) {
        return Jsoup.connect(url)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) " +
                        "Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .timeout(10000)
                .followRedirects(false);
    }

    private void parseLinks(String url) {
        try {
            Thread.sleep(150);
            Document doc = connect(url).get();
            Elements links = doc.select("a");
            links.forEach(element -> {
                String link = element.attr("abs:href");
                if (isTrueLink(link)) {
                    linkList.add(link);
                    childLinkList.add(link);
                }
            });
        } catch (InterruptedException | IOException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private Boolean isTrueLink(String link) {
        Pattern pattern = Pattern.compile(
                "^((https):/)/(" + rootUrl + ".ru)((/\\w[^/]+)*/)([\\w\\-.]+(?=/)[^#?\\s])$");
        Matcher matcher = pattern.matcher(link);
        while (matcher.find()) {
            if (!linkList.contains(link) &&
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
        }
        return false;
    }
}
