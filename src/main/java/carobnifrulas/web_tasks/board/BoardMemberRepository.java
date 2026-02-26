package carobnifrulas.web_tasks.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardMemberRepository extends JpaRepository<BoardMember, BoardMemberId> {

    Optional<BoardMember> findByIdBoardIdAndIdUserId(Long boardId, Long userId);

    boolean existsByIdBoardIdAndIdUserId(Long boardId, Long userId);

    List<BoardMember> findAllByIdBoardId(Long boardId);

    // Projekcija za prikaz članova sa user podacima
    @Query("""
        select u.id as userId,
               u.email as email,
               u.fullName as fullName,
               bm.role as role
        from BoardMember bm
        join User u on u.id = bm.id.userId
        where bm.id.boardId = :boardId
        order by
            case bm.role
                when 'OWNER' then 0
                when 'ADMIN' then 1
                when 'MEMBER' then 2
                else 3
            end,
            u.email
    """)
    List<MemberRow> findMemberRows(@Param("boardId") Long boardId);

    interface MemberRow {
        Long getUserId();
        String getEmail();
        String getFullName();
        String getRole(); // "OWNER"/"ADMIN"/...
    }
}