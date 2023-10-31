package searchengine.services;

import lombok.RequiredArgsConstructor;
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
    @Override
    public String getSnippet(String query, SearchPageIndex searchPageIndex) throws IOException {
        String[] queryArray = query.split("\\s");
        Map<Integer, String> queryLemmaMap = new HashMap<>();
        String[] contentArray = searchPageIndex.getPage().getContent().split("\\s");
        Map<Integer, String> contentLemmaMap = new HashMap<>();
        Map<Integer, String> contentStringMap = new HashMap<>();
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
            contentStringMap.put(i, contentArray[i]);
            Set<String> lemmaSet = lemmaFinderService.getLemmaSet(contentArray[i]);
            if (lemmaSet.isEmpty()) {
                continue;
            }
            String contentPartLemma = lemmaSet.stream().toList().get(0);

            contentLemmaMap.put(i, contentPartLemma);
        }
        System.out.println(contentLemmaMap.size());

        String snippet = "";
        int snipFrom = 0;
        int snipTo = 30;
        Set<Integer> snipSet = new HashSet<>();
        for (Map.Entry<Integer, String> entryQuery : queryLemmaMap.entrySet()) {
            for (Map.Entry<Integer, String> entryContent : contentLemmaMap.entrySet()) {
                if (entryQuery.getValue().equals(entryContent.getValue())) {
                    snipFrom = entryContent.getKey() - 15;
                    snipSet.add(entryContent.getKey());
                    if (snipFrom < 0) {
                        snipFrom = 0;
                    }
                    snipTo = snipFrom + 30;
                }
            }
        }
        for (int i = snipFrom; i <= snipTo; i++) {
            try {
                if (contentStringMap.get(i).isEmpty()){
                    continue;
                }
            if (i % 10 == 0) {
                snippet = snippet.concat("<br>");
            }
            if (snipSet.contains(i)) {
                snippet = snippet.concat(" ").concat("<b>").concat(contentStringMap.get(i).concat("</b>"));
            } else {
                snippet = snippet.concat(" ").concat(contentStringMap.get(i)).concat(" ");
            }
            } catch(NullPointerException ne) {
                ne.printStackTrace();
            }
        }
        return snippet;
    }
}
