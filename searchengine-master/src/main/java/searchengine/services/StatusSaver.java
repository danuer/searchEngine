package searchengine.services;

import lombok.Data;
import searchengine.model.SiteRepository;
import searchengine.model.StatusList;

import java.util.List;
import java.util.TreeSet;

@Data
public class StatusSaver extends Thread {
    public StatusSaver(List<Parser> parserList, SiteRepository siteRepository) {
        this.parserList = parserList;
        this.siteRepository = siteRepository;
    }

    private List<Parser> parserList;
    private SiteRepository siteRepository;
    private TreeSet<String> linkTreeSet;

    @Override
    public void run() {
        Thread.currentThread().setName("StatusSaverThread");
        linkTreeSet = new TreeSet<>();
        for (Parser parser : parserList) {
            linkTreeSet.addAll(parser.join());
            searchengine.model.Site modelSite = siteRepository.findSiteByUrl(parser.getRootUrl());
            modelSite.setStatus(StatusList.INDEXED);
            modelSite.setStatusTime(System.currentTimeMillis());
            siteRepository.save(modelSite);
        }
    }
}
