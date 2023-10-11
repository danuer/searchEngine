package searchengine.dto.search;

import lombok.Data;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.Map;

@Data
public class SearchPageIndex {
    private Page page;
    private Map<Lemma,Float> lemmaRankMap;
    private float absRelevance;
    private float relRelevance;
}
