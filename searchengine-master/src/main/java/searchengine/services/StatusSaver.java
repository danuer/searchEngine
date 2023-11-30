package searchengine.services;

import searchengine.model.SiteEntity;
import searchengine.model.repositorys.SiteRepository;
import searchengine.model.StatusList;

import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;

import static searchengine.services.PageIndexerServiceImpl.pageMapForIndexer;

public class StatusSaver extends Thread {

    private final List<Parser> parserList;
    private final SiteRepository siteRepository;
    private final ForkJoinPool fjp;
    private final PageIndexerService pageIndexerService;

    public StatusSaver(PageIndexerService pageIndexerService, ForkJoinPool fjp, List<Parser> parserList, SiteRepository siteRepository) {
        this.parserList = parserList;
        this.siteRepository = siteRepository;
        this.fjp = fjp;
        this.pageIndexerService = pageIndexerService;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("StatusSaverThread");
        TreeSet<String> linkTreeSet = new TreeSet<>();
        for (Parser parser : parserList) {
            SiteEntity modelSiteEntity = siteRepository.findSiteByUrl(parser.getRootUrl());
            try {
                linkTreeSet.addAll(fjp.invoke(parser));
            } catch (Exception ex) {
                ex.printStackTrace();
                modelSiteEntity.setStatus(StatusList.FAILED);
                modelSiteEntity.setStatusTime(System.currentTimeMillis());
                modelSiteEntity.setLastError("Ошибка индексации");
                return;
            }
            boolean result = pageIndexerService.pageIndexer();
            if (result) {
                modelSiteEntity.setStatus(StatusList.INDEXED);
                modelSiteEntity.setStatusTime(System.currentTimeMillis());
                siteRepository.save(modelSiteEntity);
                pageMapForIndexer.clear();
            }
        }
    }
}
