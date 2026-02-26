package carobnifrulas.web_tasks.board;

import carobnifrulas.web_tasks.list.ListService;
import carobnifrulas.web_tasks.security.model.AppUserService;
import carobnifrulas.web_tasks.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoardService {

    private final BoardRepository boards;
    private final BoardMemberRepository members;
    private final ListService listService;
    private final AppUserService userService;


    public BoardService(BoardRepository boards, BoardMemberRepository members, ListService listService, AppUserService userService) {
        this.boards = boards;
        this.members = members;
        this.listService = listService;
        this.userService = userService;
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
//        bm.setRole("OWNER");
//        members.save(bm);
        bm.setRole("OWNER");
        members.save(bm);
        listService.createDefaultListsIfMissing(b.getId(), creatorUserId);

        return b;
    }

    public Board requireMemberBoard(Long boardId, Long userId) {

        boolean globalAdmin = userService.findById(userId)
                .map(u -> "admin@local".equalsIgnoreCase(u.getEmail()))
                .orElse(false);

        if (!globalAdmin) {
            members.findByIdBoardIdAndIdUserId(boardId, userId)
                    .orElseThrow(() -> new IllegalStateException("Nemaš pristup ovom boardu."));
        }

        return boards.findById(boardId)
                .orElseThrow(() -> new IllegalStateException("Board ne postoji."));
    }


    public List<Board> listBoardsFor(User loggedUser) {
        boolean globalAdmin = "admin@local".equalsIgnoreCase(loggedUser.getEmail());

        if (globalAdmin) {
            return boards.findAllByOrderByIdDesc(); // ili po name, kako želiš
        }
        return boards.findBoardsForUser(loggedUser.getId()); // tvoja postojeća logika
    }

}