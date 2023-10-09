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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static searchengine.services.Parser.connect;


@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final PageIndexerService pageIndexerService;
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
    private final GetResponse getResponse = new GetResponse();
    private final PostResponse postResponse = new PostResponse();
    private List<Site> sitesList;
    private ForkJoinPool fjp;

    @Override
    public ResponseEntity<GetResponse> startIndexing() {
        sitesList = sites.getSites();
        List<Parser> parserList = new ArrayList<>();
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
                cleanSiteRepository(name);
                saveNewSite(name, url);
                Parser parser = new Parser(pageIndexerService, url, siteRepository, pageRepository, lemmaRepository, indexRepository);
                parserList.add(parser);
            }
            StatusSaver ss = new StatusSaver(fjp, parserList, siteRepository);
            ss.start();
            getResponse.setResult(true);
            getResponse.setError("");
        }
        return new ResponseEntity<>(getResponse, HttpStatus.OK);
    }

    private void cleanSiteRepository(String name) {
        Optional<searchengine.model.Site> siteId = siteRepository.findSiteByName(name);
        siteId.ifPresent(site -> siteRepository.deleteAllById(site.getId()));
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
    public ResponseEntity<GetResponse> stopIndexing() throws InterruptedException {
        sitesList = sites.getSites();
        if (fjp != null) {
            if (fjp.getActiveThreadCount() != 0) {
                isInterrupted = true;
                Thread.sleep(2000);
                fjp.shutdownNow();
                for (Site site : sitesList) {
                    String name = site.getName();
                    Optional<searchengine.model.Site> editSiteOpt = siteRepository.findSiteByName(name);
                    editSiteOpt.ifPresent(site1 -> {
                        if (!site1.getStatus().equals(StatusList.INDEXED)) {
                            site1.setStatus(StatusList.FAILED);
                            site1.setStatusTime(System.currentTimeMillis());
                            site1.setLastError("Индексация остановлена пользователем");
                            siteRepository.save(site1);
                        }
                    });

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
        String regex = "https?\\:\\/\\/[^\\/]+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        if(matcher.find()) {
            String rootUrl = matcher.group(0);
            for (Site site : sitesList) {
                if (rootUrl.equals(site.getUrl())) {
                    Optional<searchengine.model.Site> modelSiteOpt = siteRepository.findSiteByName(site.getName());
                    if (modelSiteOpt.isEmpty()) {
                        searchengine.model.Site modelSite = saveNewSite(site.getName(), site.getUrl());
                    }
                    Parser parser = new Parser(pageIndexerService, url, siteRepository, pageRepository, lemmaRepository, indexRepository);
                    Document doc = connect(url).get();
                    String entityUrl = url.substring(matcher.end());
                    Optional<Page> pageOpt = pageRepository.findByPath(entityUrl);
                    pageOpt.ifPresent(page -> pageRepository.deleteAllById(page.getId()));
                    Page page = parser.savePage(doc, url, rootUrl);
                    pageIndexerService.pageIndexer(url, rootUrl, page);
                    postResponse.setResult(true);
                    postResponse.setError("");
                }
            }
        }
        return new ResponseEntity<>(postResponse, HttpStatus.OK);
    }
}
