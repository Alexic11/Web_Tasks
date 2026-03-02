package carobnifrulas.web_tasks.card.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardActivityRepository extends JpaRepository<CardActivity, Long> {

    List<CardActivity> findByCardIdOrderByCreatedAtDesc(Long cardId);
}
