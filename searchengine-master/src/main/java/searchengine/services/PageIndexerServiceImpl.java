package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.model.repositorys.IndexRepository;
import searchengine.model.repositorys.LemmaRepository;
import searchengine.model.repositorys.PageRepository;
import searchengine.model.repositorys.SiteRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PageIndexerServiceImpl implements PageIndexerService {

    @Autowired
    private final LemmaFinderService lemmaFinderService;
    private String url;
    private String rootUrl;
    private int pageId;
    private Page page;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private PageRepository pageRepository;
    public static Map<String, Integer> pageMapForIndexer;

    @Bean
    public Map<String, Integer> getMap() {
        pageMapForIndexer = new ConcurrentSkipListMap<>();
        return pageMapForIndexer;
    }


    @Override
    public boolean pageIndexer() {
        Thread.currentThread().setName("PageIndexerThread");
        for (Map.Entry<String, Integer> pageEntry : pageMapForIndexer.entrySet()) {
            url = pageEntry.getKey();
            pageId = pageEntry.getValue();
            Optional<Page> pageOpt = pageRepository.findById(pageId);
            pageOpt.ifPresent(page1 -> page = pageOpt.get());
            rootUrl = getRootUrl(url);
            try {
                String text = Jsoup.parse(page.getContent()).text();
                Map<String, Integer> lemmas = lemmaFinderService.collectLemmas(text);
                if (!lemmas.isEmpty()) {
                    for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                        Lemma lemmaEntity = lemmaRepository.searchByLemmaAndSiteEntity(entry.getKey(), page.getSiteEntity());
                        if (lemmaEntity != null) {
                            int lemmaFrequency = lemmaEntity.getFrequency() + 1;
                            lemmaEntity.setFrequency(lemmaFrequency);
                        } else {
                            lemmaEntity = new Lemma();
                            lemmaEntity.setLemma(entry.getKey());
                            lemmaEntity.setFrequency(1);
                            lemmaEntity.setSiteEntity(page.getSiteEntity());
                        }

                        Lemma savedLemma;
                        synchronized (lemmaRepository) {
                            savedLemma = lemmaRepository.save(lemmaEntity);
                        }
                        Index indexEntity = new Index();
                        indexEntity.setPage(page);
                        indexEntity.setLemma(savedLemma);
                        indexEntity.setRank(entry.getValue());
                        Index savedIndex = indexRepository.save(indexEntity);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private String getRootUrl(String url) {
        String rootUrl = "";
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String regex = "https?\\:\\/\\/[^\\/]+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
//        System.out.println(url);
        if (matcher.find()) {
            rootUrl = matcher.group(0);
//            System.out.println(rootUrl);
        }
        return rootUrl;
    }
}
