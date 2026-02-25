package carobnifrulas.web_tasks.list;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "lists")
@Getter @Setter
public class ListEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal position;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}