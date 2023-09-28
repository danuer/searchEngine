package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity(name = "`index`")
@Getter
@Setter
@RequiredArgsConstructor
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @JoinColumn(name = "page_id", nullable = false)
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Page page;

    @JoinColumn(name = "lemma_id", nullable = false)
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Lemma lemma;

    @Column(name = "`rank`",columnDefinition = "FLOAT", nullable = false)
    private float rank;
}
