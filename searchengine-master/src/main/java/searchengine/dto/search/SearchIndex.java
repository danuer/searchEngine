package searchengine.dto.search;

import lombok.Data;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

@Data
public class SearchIndex {
    private Index index;
    private Lemma lemma;
    private Integer pageId;
    private float rank;
}
