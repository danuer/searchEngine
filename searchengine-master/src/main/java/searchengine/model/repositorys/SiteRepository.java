package searchengine.model.repositorys;

import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface SiteRepository extends CrudRepository<Site, Integer> {
    Optional<Site> findSiteByName(String name);
    Site findSiteByUrl(String url);
    void deleteAllByName(String name);
    void deleteAllById(Integer id);
}
