package searchengine.model;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table (name = "page", indexes = @Index(name = "page_index", columnList = "path, site_id", unique = true))
@Getter
@Setter
@RequiredArgsConstructor
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @JoinColumn(name = "site_id", nullable = false)
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Site site;

    @Column(columnDefinition = "VARCHAR(512)", nullable = false)
    private String path;

    @Column(columnDefinition = "INT", nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

}
