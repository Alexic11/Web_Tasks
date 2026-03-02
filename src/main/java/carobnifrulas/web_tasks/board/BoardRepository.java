package carobnifrulas.web_tasks.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    @Query("""
        select b from Board b
        where b.id in (
            select bm.id.boardId from BoardMember bm where bm.id.userId = :userId
        )
        order by b.id desc
    """)
    List<Board> findBoardsForUser(Long userId);

    List<Board> findAllByOrderByIdDesc();



    @Query("""
        select b from Board b
        where b.archivedAt is null
          and b.id in (
            select bm.id.boardId from BoardMember bm where bm.id.userId = :userId
          )
        order by b.id desc
    """)
    List<Board> findActiveBoardsForUser(Long userId);

    @Query("""
        select b from Board b
        where b.archivedAt is not null
          and b.id in (
            select bm.id.boardId from BoardMember bm where bm.id.userId = :userId
          )
        order by b.archivedAt desc, b.id desc
    """)
    List<Board> findArchivedBoardsForUser(Long userId);

    List<Board> findAllByArchivedAtIsNullOrderByIdDesc();
    List<Board> findAllByArchivedAtIsNotNullOrderByArchivedAtDesc();
}