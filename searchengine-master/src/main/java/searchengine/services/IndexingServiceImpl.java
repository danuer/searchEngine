package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.response.GetResponse;
import searchengine.dto.response.PostResponse;
import searchengine.model.*;
import searchengine.model.repositorys.IndexRepository;
import searchengine.model.repositorys.LemmaRepository;
import searchengine.model.repositorys.PageRepository;
import searchengine.model.repositorys.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static searchengine.services.Parser.connect;


@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    static boolean isInterrupted;
    private GetResponse getResponse = new GetResponse();
    private PostResponse postResponse = new PostResponse();
    private List<Site> sitesList;
    ;
    private ForkJoinPool fjp;
    private List<Parser> parserList;

    @Override
    public ResponseEntity<GetResponse> startIndexing() {
        sitesList = sites.getSites();
        parserList = new ArrayList<>();
        if (fjp != null) {
            if (fjp.getActiveThreadCount() > 0) {
                getResponse.setResult(false);
                getResponse.setError("Индексация уже запущена");
            }
        } else {
            fjp = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
            for (Site site : sitesList) {
                String url = site.getUrl();
                String name = site.getName();
                isInterrupted = false;
                cleanRepo(name);
                saveNewSite(name, url);
                Parser parser = new Parser(url, siteRepository, pageRepository, lemmaRepository, indexRepository);
                parserList.add(parser);
            }
            StatusSaver ss = new StatusSaver(fjp, parserList, siteRepository);
            ss.start();
            getResponse.setResult(true);
            getResponse.setError("");
        }
        return new ResponseEntity<>(getResponse, HttpStatus.OK);
    }

    private void cleanRepo(String name) {
        searchengine.model.Site siteId = siteRepository.findSiteByName(name);
        if (siteId != null) {
            siteRepository.deleteAllById(siteId.getId());
        }
    }

    private searchengine.model.Site saveNewSite(String name, String url) {
        searchengine.model.Site modelSite = new searchengine.model.Site();
        modelSite.setName(name);
        modelSite.setUrl(url);
        modelSite.setStatus(StatusList.INDEXING);
        modelSite.setStatusTime(System.currentTimeMillis());
        searchengine.model.Site savedRootSite = siteRepository.save(modelSite);
        System.out.println(savedRootSite.getId());
        return savedRootSite;
    }

    private void writeErrToRepo(searchengine.model.Site modelSite) {
        modelSite.setStatus(StatusList.FAILED);
        modelSite.setStatusTime(System.currentTimeMillis());
        modelSite.setLastError("Ошибка индексации");
        siteRepository.save(modelSite);
    }

    @Override
    public ResponseEntity<GetResponse> stopIndexing() {
        sitesList = sites.getSites();
        if (fjp != null) {
            if (fjp.getActiveThreadCount() != 0) {
                isInterrupted = true;
                fjp.shutdownNow();
                for (Site site : sitesList) {
                    String name = site.getName();
                    searchengine.model.Site editSite = siteRepository.findSiteByName(name);
                    if (!editSite.getStatus().equals(StatusList.INDEXED)) {
                        editSite.setStatus(StatusList.FAILED);
                        editSite.setStatusTime(System.currentTimeMillis());
                        editSite.setLastError("Индексация остановлена пользователем");
                        siteRepository.save(editSite);
                    }
                }
//            GetResponse response = new GetResponse();
                getResponse.setResult(true);
                getResponse.setError("");
            }
        } else {
//            GetResponse response = new GetResponse();
            getResponse.setResult(false);
            getResponse.setError("Индексация не запущена");
        }
        return new ResponseEntity<>(getResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<PostResponse> indexingPage(String url) throws IOException {
        sitesList = sites.getSites();
        postResponse.setResult(false);
        postResponse.setError("Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле");
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String rootUrl = url.split("/")[0];
        for (Site site : sitesList) {
            if (rootUrl.equals(site.getUrl())) {
                searchengine.model.Site modelSite = siteRepository.findSiteByName(site.getName());
                if (modelSite == null) {
                    modelSite = saveNewSite(site.getName(), url);
                }
                Parser parser = new Parser(url, siteRepository, pageRepository, lemmaRepository, indexRepository);
                Document doc = connect(url).get();
                Page page = pageRepository.searchByPath(url);
                if (page == null) {
                    page = parser.savePage(doc, url, rootUrl);
                }
                PageIndexer indexer = new PageIndexer(url, rootUrl, siteRepository, pageRepository, page, lemmaRepository, indexRepository);
                new Thread(indexer).start();
                postResponse.setResult(true);
                postResponse.setError("");
            }
        }
        return new ResponseEntity<>(postResponse, HttpStatus.OK);
    }
}
