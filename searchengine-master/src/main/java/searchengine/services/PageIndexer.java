package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.nodes.Document;
import searchengine.model.Page;
import searchengine.model.PageRepository;
import searchengine.model.Site;
import searchengine.model.SiteRepository;

import java.io.IOException;
import java.util.*;

public class PageIndexer implements Runnable {

    Document doc;
    String url;
    String rootUrl;
    SiteRepository siteRepository;
    PageRepository pageRepository;
    Page page;

    public PageIndexer(Document doc, String url, String rootUrl, SiteRepository siteRepository, PageRepository pageRepository, Page page) {
        this.doc = doc;
        this.url = url;
        this.rootUrl = rootUrl;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.page = page;
    }

    @Override
    public void run() {

    }

}
