package searchengine.model.repositorys;

import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;

@Repository
@Transactional
public interface PageRepository extends CrudRepository<Page, Integer> {
    List<Integer> findAllByPathAndSite(String path, Site site);
    Page searchAllBySite(Site site);
    Page searchByPath(String path);


    void deleteAllBySite(Site site);
}
