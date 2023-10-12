package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
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
import searchengine.model.Page;
import searchengine.model.repositorys.IndexRepository;
import searchengine.model.repositorys.LemmaRepository;
import searchengine.model.repositorys.PageRepository;
import searchengine.model.repositorys.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final LemmaFinderService lemmaFinderService;
    private final SitesList sites;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;

    @Override
    public SearchResponse search(String query, int offset, int limit, @Nullable String siteUrl) throws IOException {
        SearchResponse searchResponse = new SearchResponse();
        List<Lemma> lemmaList;
        List<Site> sitesList = sites.getSites();
        Set<String> lemmas = lemmaFinderService.getLemmaSet(query);
        if (siteUrl != null) {
            lemmaList = searchBySite(siteUrl, lemmas);
        } else {
            lemmaList = searchAll(sitesList, lemmas);
        }
        Map<Lemma, Integer> lemmaMapByFreq = new HashMap<>();
        for (Lemma lemma : lemmaList) {
            lemmaMapByFreq.put(lemma, lemma.getFrequency());
        }
        List<Lemma> lemmaSortedList = new ArrayList<>();
        lemmaMapByFreq.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
                .forEach(entry -> lemmaSortedList.add(entry.getKey()));
        List<Index> indexList = indexRepository.findAllByLemma(lemmaSortedList.get(0));
        List<Integer> pageIdList = new ArrayList<>();
        indexList.forEach(index -> {
            pageIdList.add(index.getPage().getId());
        });
        Set<Integer> resultPageIdList = new HashSet<>();
        List<SearchIndex> searchIndexList = new ArrayList<>();
        for (Lemma lemma : lemmaSortedList) {
            List<Index> newIndexList = indexRepository.findAllByLemma(lemma);
            List<Integer> newPageIdList = new ArrayList<>();
            newIndexList.forEach(index -> {
                newPageIdList.add(index.getPage().getId());

            });
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
        for (SearchIndex si : searchIndexList) {
            System.out.println(si.getPageId() + "-" + si.getLemma().getLemma() + "-" + si.getRank() + "-" + si.getIndex());
        }
        Map<Integer, SearchPageIndex> searchPageIndexMap = new HashMap();
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
        searchPageIndexMap.entrySet().forEach(System.out::println);
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
        searchPageIndexMap.entrySet().forEach(System.out::println);

        Map<Float, SearchPageIndex> sortedByRelSPIMap = new TreeMap<>();
        for (Map.Entry<Integer, SearchPageIndex> entry : searchPageIndexMap.entrySet()) {
            SearchPageIndex spi = entry.getValue();
            float rel = spi.getRelRelevance();
            sortedByRelSPIMap.put(rel, spi);
        }

        List<SearchPageIndex> sortedByRelSPIList = new ArrayList<>();
        for (Map.Entry<Float, SearchPageIndex> entry : sortedByRelSPIMap.entrySet()) {
            SearchPageIndex spi = entry.getValue();
            sortedByRelSPIList.add(spi);
        }

        searchResponse.setResult(true);
        searchResponse.setError("");
        searchResponse.setCount(sortedByRelSPIList.size());
        ArrayList<SiteData> list = new ArrayList<>();
        for (SearchPageIndex spi : sortedByRelSPIList) {
            SiteData data = new SiteData();
            data.setSite(spi.getPage().getSite().getUrl());
            data.setSiteName(spi.getPage().getSite().getName());
            data.setUri(spi.getPage().getPath());
            Document content = Jsoup.parse(spi.getPage().getContent());
            data.setTitle(content.title());
            data.setSnippet("Фрагмент текста, в котором найдены совпадения," +
                    " <b>выделенные жирным</b>, в формате HTML");
            data.setRelevance(spi.getRelRelevance());
            list.add(data);
        }
        searchResponse.setData(list);
        return searchResponse;
    }

    private List<Lemma> searchBySite(String siteUrl, Set<String> lemmas) {
        searchengine.model.Site site = siteRepository.findSiteByUrl(siteUrl);
        List<Lemma> lemmaList = new ArrayList<>();
        for (String lemma : lemmas) {
            Optional<Lemma> lemmaOpt = lemmaRepository.findByLemmaAndSite(lemma, site);
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
