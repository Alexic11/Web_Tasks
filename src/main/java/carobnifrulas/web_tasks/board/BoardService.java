package carobnifrulas.web_tasks.board;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoardService {

    private final BoardRepository boards;
    private final BoardMemberRepository members;

    public BoardService(BoardRepository boards, BoardMemberRepository members) {
        this.boards = boards;
        this.members = members;
    }

    public List<Board> findBoardsForUser(Long userId) {
        return boards.findBoardsForUser(userId);
    }

    @Transactional
    public Board createBoard(String name, Long creatorUserId) {
        Board b = new Board();
        b.setName(name);
        b = boards.save(b);

        BoardMemberId id = new BoardMemberId(b.getId(), creatorUserId);

        BoardMember bm = new BoardMember();
        bm.setId(id);
        bm.setRole("OWNER");
        members.save(bm);
        bm.setRole("OWNER");
        members.save(bm);

        return b;
    }

    public Board requireMemberBoard(Long boardId, Long userId) {
        members.findByIdBoardIdAndIdUserId(boardId, userId)
                .orElseThrow(() -> new IllegalStateException("Nemaš pristup ovom boardu."));
        return boards.findById(boardId).orElseThrow(() -> new IllegalStateException("Board ne postoji."));
    }
}