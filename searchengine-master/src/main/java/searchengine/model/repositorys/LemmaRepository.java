package searchengine.model.repositorys;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@Transactional
public interface LemmaRepository extends CrudRepository<Lemma, Integer> {
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);
    @Query(value = "SELECT l FROM Lemma l where l.lemma =:lemma and l.site =:site")
    Lemma searchByLemmaAndSite(String lemma, Site site);
    Set<Lemma> findAllByLemma(String lemma);
    List<Lemma> findAllBySite(Site site);
}
