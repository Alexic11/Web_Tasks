package carobnifrulas.web_tasks.dashboard;

import carobnifrulas.web_tasks.dashboard.dto.BoardStatsDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DashboardRepository {

    @PersistenceContext
    private EntityManager em;

    public List<BoardStatsDto> fetchStatsForAdmin() {
        return em.createQuery("""
            SELECT new carobnifrulas.web_tasks.dashboard.dto.BoardStatsDto(
                b.id,
                b.name,
                COUNT(c),
                SUM(CASE
                        WHEN c.id IS NOT NULL
                         AND l.position < (SELECT MAX(l2.position) FROM carobnifrulas.web_tasks.list.ListEntity l2 WHERE l2.boardId = b.id)
                         AND c.archivedAt IS NULL
                    THEN 1L ELSE 0L END),
                SUM(CASE
                        WHEN c.id IS NOT NULL
                         AND l.position < (SELECT MAX(l2.position) FROM carobnifrulas.web_tasks.list.ListEntity l2 WHERE l2.boardId = b.id)
                         AND c.archivedAt IS NULL
                         AND c.dueAt IS NOT NULL
                         AND c.dueAt < CURRENT_TIMESTAMP
                    THEN 1L ELSE 0L END),
                SUM(CASE
                        WHEN c.id IS NOT NULL
                         AND l.position = (SELECT MAX(l2.position) FROM carobnifrulas.web_tasks.list.ListEntity l2 WHERE l2.boardId = b.id)
                         AND c.archivedAt IS NULL
                    THEN 1L ELSE 0L END),
                SUM(CASE WHEN c.id IS NOT NULL AND c.priority = 1 THEN 1L ELSE 0L END),
                SUM(CASE WHEN c.id IS NOT NULL AND c.priority = 2 THEN 1L ELSE 0L END),
                SUM(CASE WHEN c.id IS NOT NULL AND c.priority = 3 THEN 1L ELSE 0L END)
            )
            FROM carobnifrulas.web_tasks.board.Board b
            LEFT JOIN carobnifrulas.web_tasks.card.Card c ON c.boardId = b.id
            LEFT JOIN carobnifrulas.web_tasks.list.ListEntity l ON l.id = c.listId
            GROUP BY b.id, b.name
        """, BoardStatsDto.class).getResultList();
    }

    public List<BoardStatsDto> fetchStatsForOwner(long userId) {
        return em.createQuery("""
            SELECT new carobnifrulas.web_tasks.dashboard.dto.BoardStatsDto(
                b.id,
                b.name,
                COUNT(c),
                SUM(CASE
                        WHEN c.id IS NOT NULL
                         AND l.position < (SELECT MAX(l2.position) FROM carobnifrulas.web_tasks.list.ListEntity l2 WHERE l2.boardId = b.id)
                         AND c.archivedAt IS NULL
                    THEN 1L ELSE 0L END),
                SUM(CASE
                        WHEN c.id IS NOT NULL
                         AND l.position < (SELECT MAX(l2.position) FROM carobnifrulas.web_tasks.list.ListEntity l2 WHERE l2.boardId = b.id)
                         AND c.archivedAt IS NULL
                         AND c.dueAt IS NOT NULL
                         AND c.dueAt < CURRENT_TIMESTAMP
                    THEN 1L ELSE 0L END),
                SUM(CASE
                        WHEN c.id IS NOT NULL
                         AND l.position = (SELECT MAX(l2.position) FROM carobnifrulas.web_tasks.list.ListEntity l2 WHERE l2.boardId = b.id)
                         AND c.archivedAt IS NULL
                    THEN 1L ELSE 0L END),
                SUM(CASE WHEN c.id IS NOT NULL AND c.priority = 1 THEN 1L ELSE 0L END),
                SUM(CASE WHEN c.id IS NOT NULL AND c.priority = 2 THEN 1L ELSE 0L END),
                SUM(CASE WHEN c.id IS NOT NULL AND c.priority = 3 THEN 1L ELSE 0L END)
            )
            FROM carobnifrulas.web_tasks.board.Board b
            JOIN carobnifrulas.web_tasks.board.BoardMember bm
                ON bm.id.boardId = b.id AND bm.id.userId = :userId AND bm.role = 'OWNER'
            LEFT JOIN carobnifrulas.web_tasks.card.Card c ON c.boardId = b.id
            LEFT JOIN carobnifrulas.web_tasks.list.ListEntity l ON l.id = c.listId
            GROUP BY b.id, b.name
        """, BoardStatsDto.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public boolean hasOwnerBoards(long userId) {
        Long cnt = em.createQuery("""
            SELECT COUNT(bm)
            FROM carobnifrulas.web_tasks.board.BoardMember bm
            WHERE bm.id.userId = :userId AND bm.role = 'OWNER'
        """, Long.class)
                .setParameter("userId", userId)
                .getSingleResult();

        return cnt != null && cnt > 0;
    }
}