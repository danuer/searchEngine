package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.response.GetResponse;
import searchengine.dto.response.PostResponse;

import java.io.IOException;

public interface IndexingService {

    ResponseEntity<GetResponse> startIndexing();
    ResponseEntity<GetResponse> stopIndexing();
    ResponseEntity<PostResponse> indexingPage(String url) throws IOException;
}
