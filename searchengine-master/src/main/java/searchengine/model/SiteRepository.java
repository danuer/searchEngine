package searchengine.model;

import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiteRepository extends CrudRepository<Site, Integer> {
    Site findSiteByName(String name);
    Site findSiteByUrl(String url);

    @Transactional
    void deleteAllByName(String name);
}
