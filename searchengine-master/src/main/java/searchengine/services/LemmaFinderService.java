package searchengine.services;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface LemmaFinderService {

    Map<String, Integer> collectLemmas(String text) throws IOException;
    Set<String> getLemmaSet(String text) throws IOException;
}
