package searchengine.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.*;
import searchengine.model.repositorys.IndexRepository;
import searchengine.model.repositorys.LemmaRepository;
import searchengine.model.repositorys.PageRepository;
import searchengine.model.repositorys.SiteRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PageIndexerServiceImpl implements PageIndexerService {

    @Autowired
    private final LemmaFinderService lemmaFinderService;
//    private String url;
//    private String rootUrl;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
//    private Page page;
    //    private List<Lemma> lemmaListForSave = new ArrayList<>();
    //    private List<Index> indexListForSave = new ArrayList<>();

    @Override
    public void pageIndexer(String url, String rootUrl, Page page) {
//        Thread.currentThread().setName("PageIndexerThread");
        try {
            String text = Jsoup.parse(page.getContent()).text();
            Map<String, Integer> lemmas = lemmaFinderService.collectLemmas(text);
            if (!lemmas.isEmpty()) {
                for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                    Lemma lemmaEntity = lemmaRepository.searchByLemmaAndSite(entry.getKey(), page.getSite());
                    if (lemmaEntity != null) {
                        int lemmaFrequency = lemmaEntity.getFrequency() + 1;
                        lemmaEntity.setFrequency(lemmaFrequency);
                    } else {
                        lemmaEntity = new Lemma();
                        lemmaEntity.setLemma(entry.getKey());
                        lemmaEntity.setFrequency(1);
                        lemmaEntity.setSite(page.getSite());
                    }
//                    lemmaListForSave.add(lemmaEntity);

                    Lemma savedLemma;
                    synchronized (lemmaRepository) {
                        savedLemma = lemmaRepository.save(lemmaEntity);
                    }
                    Index indexEntity = new Index();
                    indexEntity.setPage(page);
                    indexEntity.setLemma(savedLemma);
                    indexEntity.setRank(entry.getValue());
//                    indexListForSave.add(indexEntity);
//                    synchronized (indexRepository) {
                    Index savedIndex = indexRepository.save(indexEntity);
//                    }
                }
//                synchronized (lemmaRepository) {
//                    Iterable<Lemma> savedLemmas = lemmaRepository.saveAll(lemmaListForSave);
//                }
//                indexRepository.saveAll(indexListForSave);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
