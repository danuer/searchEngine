package searchengine.model;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table (name = "lemma", indexes = @Index(name = "lemma_index", columnList = "lemma, site_id", unique = true))
@Getter
@Setter
@RequiredArgsConstructor
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @JoinColumn(name = "site_id", nullable = false)
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Site site;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(columnDefinition = "INT", nullable = false)
    private int frequency;
}
