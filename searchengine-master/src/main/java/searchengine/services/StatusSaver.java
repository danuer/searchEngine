package searchengine.services;

import searchengine.model.SiteEntity;
import searchengine.model.repositorys.SiteRepository;
import searchengine.model.StatusList;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

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
        Logger.getLogger(Thread.currentThread().getName()).info("running ");
        Set<String> linkTreeSet = new TreeSet<>();
        for (Parser parser : parserList) {
            SiteEntity modelSiteEntity = siteRepository.findSiteByUrl(parser.getRootUrl());
            try {
                Logger.getLogger(StatusSaver.class.getName()).info("StatusSaver start indexing " + parser.getRootUrl());
                linkTreeSet.addAll(fjp.invoke(parser));
            } catch (Exception ex) {
                Logger.getLogger(StatusSaver.class.getName()).info("StatusSaver was interrupted due to exception: " + ex.getMessage());
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
                Logger.getLogger(StatusSaver.class.getName()).info("Lematization for site: " + modelSiteEntity.getName() + " was finished");
                pageMapForIndexer.clear();
            }
        }
    }
}
