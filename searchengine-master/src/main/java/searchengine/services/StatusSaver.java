package searchengine.services;

import lombok.Data;
import searchengine.model.SiteRepository;
import searchengine.model.StatusList;

import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;

@Data
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
            linkTreeSet.addAll(fjp.invoke(parser));
            searchengine.model.Site modelSite = siteRepository.findSiteByUrl(parser.getRootUrl());
            modelSite.setStatus(StatusList.INDEXED);
            modelSite.setStatusTime(System.currentTimeMillis());
            siteRepository.save(modelSite);
        }
    }
}
