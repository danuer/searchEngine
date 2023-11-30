package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
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
import static searchengine.services.PageIndexerServiceImpl.pageMapForIndexer;


@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    @Autowired
    private final PageIndexerService pageIndexerService;
    private final SitesList sites;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;
    static volatile boolean isInterrupted;
    private List<Site> sitesList;
    private ForkJoinPool fjp;

    @Bean("threadPoolTaskExecutor")
    public ForkJoinPool getAsyncExecutor() {
        fjp = new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
        return fjp;
    }

    @Override
    public GetResponse startIndexing() {
        GetResponse getResponse = new GetResponse();
        sitesList = sites.getSites();
        List<Parser> parserList = new ArrayList<>();
        if (checkFjp()) {
            getResponse.setResult(false);
            getResponse.setError("Индексация уже запущена");
        } else {
            for (Site site : sitesList) {
                String url = site.getUrl();
                String name = site.getName();
                isInterrupted = false;
                cleanSiteRepository(name);
                saveNewSite(name, url);
                Parser parser = new Parser(pageIndexerService, url, siteRepository, pageRepository, lemmaRepository, indexRepository);
                parserList.add(parser);
            }
            StatusSaver ss = new StatusSaver(pageIndexerService, fjp, parserList, siteRepository);
            ss.start();
            getResponse.setResult(true);
            getResponse.setError("");
        }
        return getResponse;
    }

    private void cleanSiteRepository(String name) {
        Optional<SiteEntity> siteId = siteRepository.findSiteByName(name);
        siteId.ifPresent(site -> siteRepository.deleteAllById(site.getId()));
    }

    private SiteEntity saveNewSite(String name, String url) {
        SiteEntity modelSiteEntity = new SiteEntity();
        modelSiteEntity.setName(name);
        modelSiteEntity.setUrl(url);
        modelSiteEntity.setStatus(StatusList.INDEXING);
        modelSiteEntity.setStatusTime(System.currentTimeMillis());
        SiteEntity savedRootSiteEntity = siteRepository.save(modelSiteEntity);
        System.out.println(savedRootSiteEntity.getId());
        return savedRootSiteEntity;
    }

    private void writeErrToRepo(SiteEntity siteEntity) {
        siteEntity.setStatus(StatusList.FAILED);
        siteEntity.setStatusTime(System.currentTimeMillis());
        siteEntity.setLastError("Ошибка индексации");
        siteRepository.save(siteEntity);
    }

    @Override
    public GetResponse stopIndexing() throws InterruptedException {
        GetResponse getResponse = new GetResponse();
        sitesList = sites.getSites();
        if (checkFjp()) {
            isInterrupted = true;
            Thread.sleep(2000);
            fjp.shutdownNow();
            for (Site site : sitesList) {
                String name = site.getName();
                Optional<SiteEntity> editSiteOpt = siteRepository.findSiteByName(name);
                editSiteOpt.ifPresent(site1 -> {
                    if (!site1.getStatus().equals(StatusList.INDEXED)) {
                        site1.setStatus(StatusList.FAILED);
                        site1.setStatusTime(System.currentTimeMillis());
                        site1.setLastError("Индексация остановлена пользователем");
                        siteRepository.save(site1);
                    }
                });

            }
            getResponse.setResult(true);
            getResponse.setError("");
        } else {
            getResponse.setResult(false);
            getResponse.setError("Индексация не запущена");
        }
        return getResponse;
    }

    @Override
    public PostResponse indexingPage(String url) throws IOException {
        PostResponse postResponse = new PostResponse();
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
        System.out.println(url);
        if (matcher.find()) {
            String rootUrl = matcher.group(0);
            System.out.println(rootUrl);
            for (Site site : sitesList) {
                if (rootUrl.equals(site.getUrl())) {
                    Optional<SiteEntity> optionalSiteEntity = siteRepository.findSiteByName(site.getName());
                    if (optionalSiteEntity.isEmpty()) {
                        SiteEntity siteEntity = saveNewSite(site.getName(), site.getUrl());
                    }
                    Parser parser = new Parser(pageIndexerService, url, siteRepository, pageRepository, lemmaRepository, indexRepository);
                    Document doc = connect(url).get();
                    String entityUrl = url.substring(matcher.end());
                    Optional<Page> pageOpt = pageRepository.findByPath(entityUrl);
                    pageOpt.ifPresent(page -> pageRepository.deleteAllById(page.getId()));
                    Page page = parser.savePage(doc, url, rootUrl);
                    pageMapForIndexer.put(url, page.getId());
                    pageIndexerService.pageIndexer();
                    postResponse.setResult(true);
                    postResponse.setError("");
                }
            }
        }
        return postResponse;
    }

    private boolean checkFjp() {
        if (fjp.getActiveThreadCount() > 0) {
            return true;
        }
        return false;
    }
}
