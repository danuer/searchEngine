package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.response.GetResponse;
import searchengine.dto.response.PostResponse;
import searchengine.dto.response.SearchResponse;
import searchengine.dto.response.SiteData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.StatisticsService;

import java.util.ArrayList;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<GetResponse> startIndexing() {
        GetResponse response = new GetResponse();
        response.setResult(true);
        response.setError("");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<GetResponse> stopIndexing() {
        GetResponse response = new GetResponse();
        response.setResult(true);
        response.setError("");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{indexPage}")
    public ResponseEntity<PostResponse> addPage() {
        PostResponse response = new PostResponse();
        response.setResult(true);
        response.setError("");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{search}")
    public ResponseEntity<SearchResponse> search(@PathVariable String search ) {
        SearchResponse response = new SearchResponse();
        response.setResult(true);
        response.setError("");
        response.setCount(2);
        SiteData data = new SiteData();
        data.setSite("http://www.site.com");
        data.setSiteName("Имя сайта");
        data.setUri("/path/to/page/6784");
        data.setTitle("Заголовок страницы, которую выводим");
        data.setSnippet("Фрагмент текста, в котором найдены совпадения," +
                " <b>выделенные жирным</b>, в формате HTML");
        data.setRelevance(0.93362);
        ArrayList<SiteData> list = new ArrayList<>();
        list.add(data);
        response.setData(list);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
