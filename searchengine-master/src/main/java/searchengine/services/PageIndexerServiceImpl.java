package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.repositorys.IndexRepository;
import searchengine.model.repositorys.LemmaRepository;
import searchengine.model.repositorys.PageRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class PageIndexerServiceImpl implements PageIndexerService {

    private final LemmaFinderService lemmaFinderService;
    private Page page;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    public static Map<String, Integer> pageMapForIndexer;

    @Bean
    public Map<String, Integer> getMap() {
        pageMapForIndexer = new ConcurrentSkipListMap<>();
        return pageMapForIndexer;
    }

    @Override
    public boolean pageIndexer() {
        Thread.currentThread().setName("PageIndexerThread");
        Logger.getLogger(PageIndexerService.class.getName()).info("Lemmatization was started");
        for (Map.Entry<String, Integer> pageEntry : pageMapForIndexer.entrySet()) {
            int pageId = pageEntry.getValue();
            Optional<Page> pageOpt = pageRepository.findById(pageId);
            pageOpt.ifPresent(page1 -> page = pageOpt.get());
            try {
                String text = Jsoup.parse(page.getContent()).text();
                Map<String, Integer> lemmas = lemmaFinderService.collectLemmas(text);
                if (!lemmas.isEmpty()) {
                    lemmaAndIndexSaver(lemmas);
                }
            } catch (Exception e) {
                Logger.getLogger(PageIndexerService.class.getName()).info("Lemmatization was interrupted due to: " + e.getMessage());
            }
        }
        return true;
    }

    private void lemmaAndIndexSaver(Map<String, Integer> lemmas) {
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
            indexRepository.save(indexEntity);
        }
    }
}
