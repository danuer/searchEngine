package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchPageIndex;
import searchengine.model.repositorys.IndexRepository;
import searchengine.model.repositorys.LemmaRepository;
import searchengine.model.repositorys.PageRepository;
import searchengine.model.repositorys.SiteRepository;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SnippetServiceImpl implements SnippetService{
    private final LemmaFinderService lemmaFinderService;
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    @Override
    public String getSnippet(String query, SearchPageIndex searchPageIndex) throws IOException {
        String[] queryArray = query.split("\\s");
        Map<Integer, String> queryLemmaMap = new HashMap<>();
        String[] contentArray = searchPageIndex.getPage().getContent().split("\\s");
        Map<Integer, String> contentLemmaMap = new HashMap<>();

        for (int i = 0; i < queryArray.length; i++) {
            Set<String> lemmaSet = lemmaFinderService.getLemmaSet(queryArray[i]);
            if (lemmaSet.isEmpty()) {
                continue;
            }
            String queryPartLemma = lemmaSet.stream().toList().get(0);
            System.out.println(queryPartLemma);
            queryLemmaMap.put(i, queryPartLemma);
        }
        System.out.println(queryLemmaMap.size());
        for (int i = 0; i < contentArray.length; i++) {
            Set<String> lemmaSet = lemmaFinderService.getLemmaSet(contentArray[i]);
            if (lemmaSet.isEmpty()) {
                continue;
            }
            String contentPartLemma = lemmaSet.stream().toList().get(0);

            contentLemmaMap.put(i, contentPartLemma);
        }
        System.out.println(contentLemmaMap.size());
        String snippet = "snippet: <b>";
        for (Map.Entry<Integer, String> entryQuery : queryLemmaMap.entrySet()) {
            for (Map.Entry<Integer, String> entryContent : contentLemmaMap.entrySet()) {
                if (entryQuery.getValue().equals(entryContent.getValue())) {
                    snippet = snippet.concat(entryContent.getValue());
                }
            }
        }
        snippet = snippet.concat("<b>");
        return snippet;
    }
}
