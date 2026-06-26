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
        Integer getPriority();
        java.time.LocalDateTime getDueAt();
        Long getBoardId();
        String getBoardName();
        Long getListId();
        String getListTitle();
    }

    @Query("""
    select
      c.id as cardId,
      c.title as title,
      c.priority as priority,
      c.dueAt as dueAt,
      b.id as boardId,
      b.name as boardName,
      l.id as listId,
      l.title as listTitle
    from Card c
      join Board b on b.id = c.boardId
      join ListEntity l on l.id = c.listId
    where c.assignedTo = :userId
      and c.archivedAt is null
    order by c.updatedAt desc
""")
    List<MyTaskRow> findMyTasks(Long userId);


    interface ArchivedCardRow {
        Long getCardId();
        String getTitle();
        Integer getPriority();
        java.time.LocalDateTime getDueAt();
        java.time.LocalDateTime getArchivedAt();
        Long getListId();
        String getListTitle();
        Long getAssigneeId();
        String getAssigneeName();
        String getAssigneeEmail();
    }

    @Query("""
    select
      c.id as cardId,
      c.title as title,
      c.priority as priority,
      c.dueAt as dueAt,
      c.archivedAt as archivedAt,
      l.id as listId,
      l.title as listTitle,
      u.id as assigneeId,
      u.fullName as assigneeName,
      u.email as assigneeEmail
    from Card c
      join ListEntity l on l.id = c.listId
      left join User u on u.id = c.assignedTo
    where c.boardId = :boardId
      and c.archivedAt is not null
    order by c.archivedAt desc, c.id desc
""")
    List<ArchivedCardRow> findArchivedCardsForBoard(Long boardId);



    public interface TaskRow {
        Long getCardId();
        String getTitle();
        java.time.LocalDateTime getDueAt();

        Long getBoardId();
        String getBoardName();

        Long getListId();
        String getListTitle();

        Long getAssigneeId();
        String getAssigneeName();
        String getAssigneeEmail();
    }

    @org.springframework.data.jpa.repository.Query("""
    select
      c.id as cardId,
      c.title as title,
      c.dueAt as dueAt,
      b.id as boardId,
      b.name as boardName,
      l.id as listId,
      l.title as listTitle,
      u.id as assigneeId,
      u.fullName as assigneeName,
      u.email as assigneeEmail
    from Card c
      join ListEntity l on l.id = c.listId
      join Board b on b.id = c.boardId
      left join User u on u.id = c.assignedTo
    where (:assigneeId is null or c.assignedTo = :assigneeId)
      and c.archivedAt is null
""")
    java.util.List<TaskRow> findTaskRows(Long assigneeId);



    @Query("""
    select count(c) from Card c
    where c.boardId = :boardId
      and c.archivedAt is null
      and c.listId <> :doneListId
""")
    long countNotInDone(Long boardId, Long doneListId);


    @Query("""
    select count(c) from Card c
    where c.boardId = :boardId
      and c.archivedAt is null
      and c.listId <> :doneListId
""")
    long countOpenInBoard(Long boardId, Long doneListId);


}