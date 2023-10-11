package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.response.GetResponse;
import searchengine.dto.response.PostResponse;
import searchengine.dto.response.SearchResponse;

import java.io.IOException;

public interface IndexingService {

    GetResponse startIndexing();
    GetResponse stopIndexing() throws InterruptedException;
    PostResponse indexingPage(String url) throws IOException;

}
