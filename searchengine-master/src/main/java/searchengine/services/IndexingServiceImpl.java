package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.response.GetResponse;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;


@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService  {

    private final SitesList sites;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    static boolean isInterrupted;


    List<Parser> parserList = new ArrayList<>();
    List<ForkJoinPool> fjpList = new ArrayList<>();
    ForkJoinPool fjp = new ForkJoinPool();
    ForkJoinTask<Parser> taskList;

    GetResponse response = new GetResponse();
    @Override
    public ResponseEntity<GetResponse> startIndexing() throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        List<Site> sitesList = sites.getSites();
        isInterrupted = false;
        if (fjp.isShutdown()) {
            for (int i = 0; i < sitesList.size(); i++) {
                Site site = sitesList.get(i);
                String url = site.getUrl();
                String name = site.getName();
                Parser parser = new Parser(url, siteRepository, pageRepository);
                parserList.add(parser);

                fjpList.add(fjp);
                fjp.execute(parser);
            }
            response.setResult(true);
            response.setError("");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.setResult(false);
            response.setError("Индексация уже запущена");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

//        for (Site site : sitesList) {
//            String url = site.getUrl();
//            String name = site.getName();
//            Parser parser = new Parser(url, siteRepository, pageRepository);
//            ForkJoinPool fjp = new ForkJoinPool();
//            fjp.invoke(parser);
//            }

        }


    @Override
    public ResponseEntity<GetResponse> stopIndexing() {
        isInterrupted = true;
        fjp.shutdownNow();


        GetResponse response = new GetResponse();
        response.setResult(true);
        response.setError("");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
