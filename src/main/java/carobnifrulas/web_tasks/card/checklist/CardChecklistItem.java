package carobnifrulas.web_tasks.card.checklist;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_checklist_items")
@Getter
@Setter
public class CardChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false)
    private Boolean done = false;

    @Column(nullable = false)
    private Integer position = 1000;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public boolean isDone() {
        return Boolean.TRUE.equals(done);
    }
}
