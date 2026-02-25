package carobnifrulas.web_tasks.list;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListRepository extends JpaRepository<ListEntity, Long> {
    List<ListEntity> findByBoardIdOrderByPositionAsc(Long boardId);
}