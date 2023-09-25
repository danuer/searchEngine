package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity(name = "`index`")
@Getter
@Setter
@RequiredArgsConstructor
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @JoinColumn(name = "page_id", nullable = false)
    @ManyToOne//(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Page page;

    @JoinColumn(name = "lemma_id", nullable = false)
    @ManyToOne//(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Lemma lemma;

    @Column(name = "`rank`",columnDefinition = "FLOAT", nullable = false)
    private float rank;
}
