package searchengine.model.repositorys;

import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
@Transactional
public interface SiteRepository extends CrudRepository<SiteEntity, Integer> {
    Optional<SiteEntity> findSiteByName(String name);
    SiteEntity findSiteByUrl(String url);
    void deleteAllByName(String name);
    void deleteAllById(Integer id);
}
