package searchengine.dto.response;

import lombok.Data;

import java.util.ArrayList;

@Data
public class SearchResponse {
    private boolean result;
    private int count;
    private ArrayList<SiteData> data;
    private String error;
}
