package searchengine.model;

import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {
    List<Integer> findAllBySiteId(Integer id);
    Page searchAllBySite(Site site);
    Page searchByPath(String path);

    @Transactional
    void deleteAllBySite(Site site);
}
