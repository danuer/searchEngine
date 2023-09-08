package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.response.GetResponse;

import java.io.IOException;

public interface IndexingService {

    ResponseEntity<GetResponse> startIndexing() throws IOException, InterruptedException;
    ResponseEntity<GetResponse> stopIndexing();
}
