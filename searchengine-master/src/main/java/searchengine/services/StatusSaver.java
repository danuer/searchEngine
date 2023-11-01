package searchengine.services;

import searchengine.model.SiteEntity;
import searchengine.model.repositorys.SiteRepository;
import searchengine.model.StatusList;

import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;

public class StatusSaver extends Thread {
    public StatusSaver(ForkJoinPool fjp, List<Parser> parserList, SiteRepository siteRepository) {
        this.parserList = parserList;
        this.siteRepository = siteRepository;
        this.fjp = fjp;
    }

    private List<Parser> parserList;
    private SiteRepository siteRepository;
    private TreeSet<String> linkTreeSet;
    private ForkJoinPool fjp;

    @Override
    public void run() {
        Thread.currentThread().setName("StatusSaverThread");
        linkTreeSet = new TreeSet<>();
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
            modelSiteEntity.setStatus(StatusList.INDEXED);
            modelSiteEntity.setStatusTime(System.currentTimeMillis());
            siteRepository.save(modelSiteEntity);
        }
    }
}
