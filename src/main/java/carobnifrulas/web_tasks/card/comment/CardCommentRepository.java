package carobnifrulas.web_tasks.card.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CardCommentRepository extends JpaRepository<CardComment, Long> {

    interface CommentRow {
        Long getId();
        Long getCardId();
        Long getAuthorUserId();

        String getAuthorName();   // u upitu mapiramo alias
        String getAuthorEmail();  // u upitu mapiramo alias

        String getBody();
        LocalDateTime getCreatedAt();
    }

    @Query("""
        select
          c.id as id,
          c.cardId as cardId,
          c.authorUserId as authorUserId,
          u.fullName as authorName,
          u.email as authorEmail,
          c.body as body,
          c.createdAt as createdAt
        from CardComment c
        join carobnifrulas.web_tasks.user.User u on u.id = c.authorUserId
        where c.cardId = :cardId
        order by c.createdAt asc, c.id asc
    """)
    List<CommentRow> findRowsForCard(@Param("cardId") Long cardId);
}