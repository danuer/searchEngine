package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.response.GetResponse;
import searchengine.model.*;
import searchengine.model.repositorys.IndexRepository;
import searchengine.model.repositorys.LemmaRepository;
import searchengine.model.repositorys.PageRepository;
import searchengine.model.repositorys.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;


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
    private final GetResponse response = new GetResponse();
    private List<Site> sitesList;
    private ForkJoinPool fjp;
    private List<Parser> parserList;

    @Override
    public ResponseEntity<GetResponse> startIndexing() throws IOException {
        sitesList = sites.getSites();
        parserList = new ArrayList<>();
        if (fjp != null) {
            if (fjp.getActiveThreadCount() > 0) {
                response.setResult(false);
                response.setError("Индексация уже запущена");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        }
            fjp = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
            for (Site site : sitesList) {
                String url = site.getUrl();
                String name = site.getName();
                isInterrupted = false;
                cleanRepo(name);
                searchengine.model.Site modelSite = new searchengine.model.Site();
                saveNewSite(name, url, modelSite);
                Parser parser = new Parser(url, siteRepository, pageRepository, lemmaRepository, indexRepository);
                parserList.add(parser);
            }
        StatusSaver ss = new StatusSaver(fjp, parserList, siteRepository);
        ss.start();
        response.setResult(true);
        response.setError("");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    private void cleanRepo(String name) {
        searchengine.model.Site siteId = siteRepository.findSiteByName(name);
        if (siteId != null) {
            siteRepository.deleteAllById(siteId.getId());
        }
    }

    private void saveNewSite(String name, String url, searchengine.model.Site modelSite) {
        modelSite.setName(name);
        modelSite.setUrl(url);
        modelSite.setStatus(StatusList.INDEXING);
        modelSite.setStatusTime(System.currentTimeMillis());
        searchengine.model.Site savedRootSite = siteRepository.save(modelSite);
        System.out.println(savedRootSite.getId());
    }

    private void writeErrToRepo(searchengine.model.Site modelSite) {
        modelSite.setStatus(StatusList.FAILED);
        modelSite.setStatusTime(System.currentTimeMillis());
        modelSite.setLastError("Ошибка индексации");
        siteRepository.save(modelSite);
    }

    @Override
    public ResponseEntity<GetResponse> stopIndexing() {
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
        GetResponse response = new GetResponse();
        response.setResult(true);
        response.setError("");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
