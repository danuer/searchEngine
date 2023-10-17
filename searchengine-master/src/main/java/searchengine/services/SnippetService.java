package searchengine.services;

import searchengine.dto.search.SearchPageIndex;

import java.io.IOException;

public interface SnippetService {
    String getSnippet(String query, SearchPageIndex searchPageIndex) throws IOException;
}
