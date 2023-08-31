package searchengine.dto.response;

import lombok.Data;

@Data
public class SiteData {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;
}
