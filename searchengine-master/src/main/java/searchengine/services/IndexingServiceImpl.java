package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.response.GetResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ForkJoinPool;


@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService  {

    private final SitesList sites;
    @Override
    public ResponseEntity<GetResponse> startIndexing() throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            String url = site.getUrl();
            Parser parser = new Parser(url);
            ForkJoinPool fjp = new ForkJoinPool();
                fjp.invoke(parser);
            }
        GetResponse response = new GetResponse();
        response.setResult(true);
        response.setError("");
        return new ResponseEntity<>(response, HttpStatus.OK);
        }


    @Override
    public ResponseEntity<GetResponse> stopIndexing() {
        return null;
    }

}
