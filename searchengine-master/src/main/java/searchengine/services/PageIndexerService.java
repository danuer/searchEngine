package searchengine.services;

import searchengine.model.Page;

public interface PageIndexerService {
    void pageIndexer(String url, String rootUrl, Page savedPage);
}
