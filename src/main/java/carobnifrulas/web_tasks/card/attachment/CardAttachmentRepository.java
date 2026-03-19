package carobnifrulas.web_tasks.card.attachment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardAttachmentRepository extends JpaRepository<CardAttachment, Long> {

    List<CardAttachment> findByCardIdOrderByCreatedAtAscIdAsc(Long cardId);

    long countByCardId(Long cardId);
}