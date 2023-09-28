package searchengine.services;

import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.*;
import searchengine.model.repositorys.IndexRepository;
import searchengine.model.repositorys.LemmaRepository;
import searchengine.model.repositorys.PageRepository;
import searchengine.model.repositorys.SiteRepository;

import java.io.IOException;
import java.util.*;

@Data
public class PageIndexer implements Runnable {

    private Document doc;
    private String url;
    private String rootUrl;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private Page page;
    private Lemma savedLemma;
    private Map<String, Integer> lemmas;

    public PageIndexer(Document doc,
                       String url,
                       String rootUrl,
                       SiteRepository siteRepository,
                       PageRepository pageRepository,
                       Page page,
                       LemmaRepository lemmaRepository,
                       IndexRepository indexRepository) {
        this.doc = doc;
        this.url = url;
        this.rootUrl = rootUrl;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.page = page;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    public void run() {
        try {
            LemmaFinder lem = LemmaFinder.getInstance();
            String text = Jsoup.parse(page.getContent()).text();
            lemmas = lem.collectLemmas(text);
            if (!lemmas.isEmpty()) {
                for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                    Lemma lemmaEntity = lemmaRepository.searchByLemmaAndSite(entry.getKey(), page.getSite());
                    if (lemmaEntity != null) {
                        int lemFreq = lemmaEntity.getFrequency() + 1;
                        lemmaEntity.setFrequency(lemFreq);
                    } else {
                        lemmaEntity = new Lemma();
                        lemmaEntity.setLemma(entry.getKey());
                        lemmaEntity.setFrequency(1);
                        lemmaEntity.setSite(page.getSite());
                    }
                    savedLemma = lemmaRepository.save(lemmaEntity);
                    Index indexEntity = new Index();
                    indexEntity.setPage(page);
                    indexEntity.setLemma(savedLemma);
                    indexEntity.setRank(entry.getValue());
                    indexRepository.save(indexEntity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
