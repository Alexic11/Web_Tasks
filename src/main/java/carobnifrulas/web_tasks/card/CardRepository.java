package carobnifrulas.web_tasks.card;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByListIdAndArchivedAtIsNullOrderByPositionAsc(Long listId);
    List<Card> findByAssignedToAndArchivedAtIsNullOrderByUpdatedAtDesc(Long userId);
}