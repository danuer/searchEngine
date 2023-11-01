package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.response.SearchResponse;
import searchengine.dto.response.SiteData;
import searchengine.dto.search.SearchIndex;
import searchengine.dto.search.SearchPageIndex;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.SiteEntity;
import searchengine.model.repositorys.IndexRepository;
import searchengine.model.repositorys.LemmaRepository;
import searchengine.model.repositorys.SiteRepository;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final LemmaFinderService lemmaFinderService;
    private final SnippetService snippetService;
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    public SearchResponse search(String query, int offset, int limit, @Nullable String siteUrl) throws IOException {
        SearchResponse searchResponse = new SearchResponse();
        List<Lemma> lemmaList;
        List<Index> indexList;
        List<Lemma> lemmaSortedList = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        Set<String> lemmas = lemmaFinderService.getLemmaSet(query);
        if (siteUrl != null) {
            lemmaList = searchBySite(siteUrl, lemmas);
        } else {
            lemmaList = searchAll(sitesList, lemmas);
        }
        if (!lemmaList.isEmpty()) {
            Map<Lemma, Integer> lemmaMapByFreq = new HashMap<>();
            for (Lemma lemma : lemmaList) {
                if (lemma.getFrequency() <= 500) {
                    lemmaMapByFreq.put(lemma, lemma.getFrequency());
                } else {
                    searchResponse.setError("Слишком большое кол-во страниц");
                }
            }
            if (lemmaMapByFreq.isEmpty()) {
                searchResponse.setResult(false);
                return searchResponse;
            }
            lemmaMapByFreq.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
                    .forEach(entry -> lemmaSortedList.add(entry.getKey()));
            indexList = indexRepository.findAllByLemma(lemmaSortedList.get(0));
        } else {
            searchResponse.setResult(false);
            searchResponse.setError("По данному запросу ничего не найдено");
            return searchResponse;
        }
        List<Integer> pageIdList = new ArrayList<>();
        indexList.forEach(index -> pageIdList.add(index.getPage().getId()));
        Set<Integer> resultPageIdList = new HashSet<>();
        List<SearchIndex> searchIndexList = new ArrayList<>();
        for (Lemma lemma : lemmaSortedList) {
            List<Index> newIndexList = indexRepository.findAllByLemma(lemma);
            List<Integer> newPageIdList = new ArrayList<>();
            newIndexList.forEach(index -> newPageIdList.add(index.getPage().getId()));
            for (Integer p : newPageIdList) {
                if (pageIdList.contains(p)) {
                    resultPageIdList.add(p);
                    SearchIndex searchIndex = new SearchIndex();
                    searchIndex.setLemma(lemma);
                    searchIndex.setPageId(p);
                    searchIndexList.add(searchIndex);
                }
            }
        }
        for (SearchIndex searchIndex : searchIndexList) {
            searchIndex.setIndex(indexRepository.findByLemmaAndPage_Id(searchIndex.getLemma(), searchIndex.getPageId()));
            float rank = searchIndex.getIndex().getRank();
            searchIndex.setRank(rank);
        }
        Map<Integer, SearchPageIndex> searchPageIndexMap = new HashMap<>();
        for (Integer pageId : resultPageIdList) {
            Map<Lemma, Float> lemmaRankMap = new HashMap<>();
            SearchPageIndex spi = new SearchPageIndex();
            float absRelevance = 0;
            for (SearchIndex si : searchIndexList) {
                if (Objects.equals(si.getPageId(), pageId)) {
                    spi.setPage(si.getIndex().getPage());
                    lemmaRankMap.put(si.getLemma(), si.getRank());
                    absRelevance += si.getRank();
                }
            }
            spi.setLemmaRankMap(lemmaRankMap);
            spi.setAbsRelevance(absRelevance);
            searchPageIndexMap.put(pageId, spi);
        }
//        searchPageIndexMap.entrySet().forEach(System.out::println);
        float maxAbsRelevance = 0;
        for (Map.Entry<Integer, SearchPageIndex> entry : searchPageIndexMap.entrySet()) {
            SearchPageIndex spi = entry.getValue();
            if (maxAbsRelevance <= spi.getAbsRelevance()) {
                maxAbsRelevance = spi.getAbsRelevance();
            }
        }
        for (Map.Entry<Integer, SearchPageIndex> entry : searchPageIndexMap.entrySet()) {
            SearchPageIndex spi = entry.getValue();
            spi.setRelRelevance(spi.getAbsRelevance() / maxAbsRelevance);
        }
//        searchPageIndexMap.entrySet().forEach(System.out::println);

        List<SearchPageIndex> sortedByRelSPIList = getSortedByRelSPIList(searchPageIndexMap);
        if (searchResponse.getError() != null) {
            searchResponse.setResult(false);
            return searchResponse;
        }
        System.out.println(sortedByRelSPIList.size());
        searchResponse.setResult(true);
        searchResponse.setError("");
        searchResponse.setCount(sortedByRelSPIList.size());
        ArrayList<SiteData> list = new ArrayList<>();
        for (SearchPageIndex spi : sortedByRelSPIList) {
            SiteData data = new SiteData();
            data.setSite(spi.getPage().getSiteEntity().getUrl());
            data.setSiteName(spi.getPage().getSiteEntity().getName());
            data.setUri(spi.getPage().getPath());
            Document content = Jsoup.parse(spi.getPage().getContent());
            data.setTitle(content.title());
            data.setSnippet(snippetService.getSnippet(query, spi));
            data.setRelevance(spi.getRelRelevance());
            list.add(data);
        }
        searchResponse.setData(list);
        return searchResponse;
    }

    private static List<SearchPageIndex> getSortedByRelSPIList(Map<Integer, SearchPageIndex> searchPageIndexMap) {
        Map<Float, SearchPageIndex> sortedByRelSPIMap = new TreeMap<>(Comparator.reverseOrder());
        for (Map.Entry<Integer, SearchPageIndex> entry : searchPageIndexMap.entrySet()) {
            SearchPageIndex spi = entry.getValue();
            float rel = spi.getAbsRelevance();
            sortedByRelSPIMap.put(rel, spi);
        }

        List<SearchPageIndex> sortedByRelSPIList = new ArrayList<>();
        for (Map.Entry<Float, SearchPageIndex> entry : sortedByRelSPIMap.entrySet()) {
            SearchPageIndex spi = entry.getValue();
            sortedByRelSPIList.add(spi);
        }
        return sortedByRelSPIList;
    }

    private List<Lemma> searchBySite(String siteUrl, Set<String> lemmas) {
        SiteEntity siteEntity = siteRepository.findSiteByUrl(siteUrl);
        List<Lemma> lemmaList = new ArrayList<>();
        for (String lemma : lemmas) {
            Optional<Lemma> lemmaOpt = lemmaRepository.findByLemmaAndSiteEntity(lemma, siteEntity);
            lemmaOpt.ifPresent(lemmaList::add);
        }
        for (Lemma lemmaEntity : lemmaList) {
            System.out.println(lemmaEntity.getLemma() + "-" + lemmaEntity.getFrequency());
        }
        return lemmaList;
    }

    private List<Lemma> searchAll(List<Site> sitesList, Set<String> lemmas) {
        List<Lemma> lemmaList = new ArrayList<>();
        for (Site site : sitesList) {
            lemmaList.addAll(searchBySite(site.getUrl(), lemmas));
        }
        return lemmaList;
    }
}
