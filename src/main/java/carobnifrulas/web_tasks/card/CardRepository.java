package carobnifrulas.web_tasks.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByListIdAndArchivedAtIsNullOrderByPositionAsc(Long listId);
    List<Card> findByAssignedToAndArchivedAtIsNullOrderByUpdatedAtDesc(Long userId);

    List<Card> findByListIdOrderByPositionAsc(Long listId);

    @Query("select max(c.position) from Card c where c.listId = :listId and c.archivedAt is null")
    BigDecimal findMaxPositionInList(Long listId);


    interface MyTaskRow {
        Long getCardId();
        String getTitle();
        LocalDateTime getDueAt();
        Instant getUpdatedAt();
        Long getBoardId();
        String getBoardName();
        Long getListId();
        String getListTitle();
    }

    @Query("""
            select c.id as cardId,
                   c.title as title,
                   c.dueAt as dueAt,
                   c.updatedAt as updatedAt,
                   c.boardId as boardId,
                   b.name as boardName,
                   c.listId as listId,
                   l.title as listTitle
            from Card c
            join Board b on b.id = c.boardId
            join ListEntity l on l.id = c.listId
            where c.assignedTo = :userId
              and c.archivedAt is null
            order by c.updatedAt desc
            """)
    List<MyTaskRow> findMyTasks(Long userId);
}