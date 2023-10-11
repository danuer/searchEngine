package searchengine.model.repositorys;

import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;

@Repository
@Transactional
public interface IndexRepository extends CrudRepository<Index, Integer> {

    Index findByLemmaAndPage(Lemma lemma, Page page);

    List<Index> findAllByLemma(Lemma lemma);

}
