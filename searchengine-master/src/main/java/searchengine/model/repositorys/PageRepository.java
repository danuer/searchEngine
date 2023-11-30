package searchengine.model.repositorys;

import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface PageRepository extends CrudRepository<Page, Integer> {
    List<Integer> findAllByPathAndSiteEntity(String path, SiteEntity siteEntity);
    List<Page> findAllBySiteEntity(SiteEntity siteEntity);
    Integer countAllBySiteEntity(SiteEntity siteEntity);
    Optional<Page> findByPath(String path);


    void deleteAllBySiteEntity(SiteEntity siteEntity);
    void deleteAllById(Integer id);
}
