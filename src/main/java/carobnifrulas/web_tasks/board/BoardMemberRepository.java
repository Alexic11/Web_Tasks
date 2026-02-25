package carobnifrulas.web_tasks.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardMemberRepository extends JpaRepository<BoardMember, BoardMember.Id> {
    Optional<BoardMember> findByIdBoardIdAndIdUserId(Long boardId, Long userId);
}