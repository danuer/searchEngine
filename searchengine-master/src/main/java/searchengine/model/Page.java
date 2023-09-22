package searchengine.model;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.*;

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
    @ManyToOne//(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private Site site;

    @Column(columnDefinition = "VARCHAR(512)", nullable = false)
    private String path;

    @Column(columnDefinition = "INT", nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

}
