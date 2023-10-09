package searchengine.model.repositorys;

import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface PageRepository extends CrudRepository<Page, Integer> {
    List<Integer> findAllByPathAndSite(String path, Site site);
    List<Page> findAllBySite(Site site);
    Optional<Page> findByPath(String path);


    void deleteAllBySite(Site site);
    void deleteAllById(Integer id);
}
