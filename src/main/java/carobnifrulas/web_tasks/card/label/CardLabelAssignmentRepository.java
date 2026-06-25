package carobnifrulas.web_tasks.card.label;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardLabelAssignmentRepository extends JpaRepository<CardLabelAssignment, CardLabelAssignmentId> {

    List<CardLabelAssignment> findByIdCardId(Long cardId);

    boolean existsByIdCardIdAndIdLabelId(Long cardId, Long labelId);

    void deleteByIdCardIdAndIdLabelId(Long cardId, Long labelId);

    void deleteByIdLabelId(Long labelId);
}
