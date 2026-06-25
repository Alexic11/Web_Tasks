package carobnifrulas.web_tasks.card.checklist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface CardChecklistRepository extends JpaRepository<CardChecklistItem, Long> {

    List<CardChecklistItem> findByCardIdOrderByPositionAscIdAsc(Long cardId);

    long countByCardId(Long cardId);

    long countByCardIdAndDoneTrue(Long cardId);

    @Query("select max(i.position) from CardChecklistItem i where i.cardId = :cardId")
    Integer findMaxPosition(Long cardId);

    interface ChecklistStatsRow {
        Long getCardId();
        Long getTotalCount();
        Long getDoneCount();
    }

    @Query("""
        select
            i.cardId as cardId,
            count(i) as totalCount,
            sum(case when i.done = true then 1L else 0L end) as doneCount
        from CardChecklistItem i
        where i.cardId in :cardIds
        group by i.cardId
    """)
    List<ChecklistStatsRow> findStatsByCardIds(Collection<Long> cardIds);
}
