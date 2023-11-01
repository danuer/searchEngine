package searchengine.model.repositorys;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Transactional
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {
    Optional<Lemma> findByLemmaAndSiteEntity(String lemma, SiteEntity siteEntity);
    @Query(value = "SELECT l FROM Lemma l where l.lemma =:lemma and l.siteEntity =:siteEntity")
    Lemma searchByLemmaAndSiteEntity(String lemma, SiteEntity siteEntity);
    Set<Lemma> findAllByLemma(String lemma);
    List<Lemma> findAllBySiteEntity(SiteEntity siteEntity);
}

