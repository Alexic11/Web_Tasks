package carobnifrulas.web_tasks.list;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ListRepository extends JpaRepository<ListEntity, Long> {
    List<ListEntity> findByBoardIdOrderByPositionAsc(Long boardId);

    @Query("select max(l.position) from ListEntity l where l.boardId = :boardId")
    BigDecimal findMaxPositionInBoard(Long boardId);

    boolean existsByBoardId(Long boardId);

    Optional<ListEntity> findFirstByBoardIdOrderByPositionDesc(Long boardId);
}