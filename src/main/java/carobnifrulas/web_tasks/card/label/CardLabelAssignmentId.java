package carobnifrulas.web_tasks.card.label;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CardLabelAssignmentId implements Serializable {

    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "label_id")
    private Long labelId;
}
