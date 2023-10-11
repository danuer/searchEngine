package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.response.SearchResponse;
import searchengine.dto.response.SiteData;
import searchengine.model.Index;
import searchengine.model.Lemma;
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
                .forEach( entry -> lemmaSortedList.add(entry.getKey()));
        List<Index> indexList = indexRepository.findAllByLemma(lemmaSortedList.get(0));
        indexList.forEach(System.out::println);
        List<Index> resultIndexList = new ArrayList<>();
        for (Lemma lemma : lemmaSortedList) {
            List<Index> newIndexList = indexRepository.findAllByLemma(lemma);
            for (Index i : newIndexList) {
                if (indexList.contains(i)) {
                    resultIndexList.add(i);
                }
            }
        }
        resultIndexList.forEach(System.out::println);

        for (Index index : resultIndexList) {
            float rank = index.getRank();
        }

//        -> {
//
//                    List<Integer> pageList = indexRepository.findAllByLemma(lemmaIntegerEntry.getKey());
//                });


        searchResponse.setResult(true);
        searchResponse.setError("");
        searchResponse.setCount(2);
        SiteData data = new SiteData();
        data.setSite("http://www.site.com");
        data.setSiteName("Имя сайта");
        data.setUri("/path/to/page/6784");
        data.setTitle("Заголовок страницы, которую выводим");
        data.setSnippet("Фрагмент текста, в котором найдены совпадения," +
                " <b>выделенные жирным</b>, в формате HTML");
        data.setRelevance(0.93362);
        ArrayList<SiteData> list = new ArrayList<>();
        list.add(data);
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
