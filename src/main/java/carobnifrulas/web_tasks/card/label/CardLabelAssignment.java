package carobnifrulas.web_tasks.card.label;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "card_label_assignments")
@Getter
@Setter
public class CardLabelAssignment {

    @EmbeddedId
    private CardLabelAssignmentId id;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
