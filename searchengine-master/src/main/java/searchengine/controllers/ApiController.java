package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.response.GetResponse;
import searchengine.dto.response.PostResponse;
import searchengine.dto.response.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<GetResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<GetResponse> stopIndexing() throws InterruptedException {
        return ResponseEntity.ok(indexingService.stopIndexing());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<PostResponse> addPage(@RequestParam("url") String url) throws IOException {
        return ResponseEntity.ok(indexingService.indexingPage(url));
    }

    @GetMapping("/{search}")
    public ResponseEntity<SearchResponse> search(@RequestParam("query") String query
                                                , @RequestParam("offset") int offset
                                                , @RequestParam("limit") int limit
                                                , @RequestParam("site") @Nullable String siteUrl) throws IOException {
        return ResponseEntity.ok(searchService.search(query, offset, limit, siteUrl));
    }

}
