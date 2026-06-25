package carobnifrulas.web_tasks.card.label;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface CardLabelRepository extends JpaRepository<CardLabel, Long> {

    List<CardLabel> findByBoardIdOrderByNameAsc(Long boardId);

    boolean existsByBoardIdAndNameIgnoreCase(Long boardId, String name);

    @Query("""
        select l
        from CardLabelAssignment a
          join CardLabel l on l.id = a.id.labelId
        where a.id.cardId = :cardId
        order by l.name asc
    """)
    List<CardLabel> findLabelsForCard(Long cardId);

    interface CardLabelRow {
        Long getCardId();
        Long getLabelId();
        Long getBoardId();
        String getName();
        String getColor();
    }

    @Query("""
        select
          a.id.cardId as cardId,
          l.id as labelId,
          l.boardId as boardId,
          l.name as name,
          l.color as color
        from CardLabelAssignment a
          join CardLabel l on l.id = a.id.labelId
        where a.id.cardId in :cardIds
        order by l.name asc
    """)
    List<CardLabelRow> findLabelRowsForCards(Collection<Long> cardIds);
}
