package searchengine.services;

import org.springframework.lang.Nullable;
import searchengine.dto.response.SearchResponse;

import java.io.IOException;

public interface SearchService {
    SearchResponse search(String query, int offset, int limit, @Nullable String url) throws IOException;
}
