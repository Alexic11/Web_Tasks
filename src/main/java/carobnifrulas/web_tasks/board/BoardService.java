package carobnifrulas.web_tasks.board;

import carobnifrulas.web_tasks.card.CardRepository;
import carobnifrulas.web_tasks.list.ListService;
import carobnifrulas.web_tasks.security.model.AppUserService;
import carobnifrulas.web_tasks.security.model.SecurityUtils;
import carobnifrulas.web_tasks.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BoardService {

    private final BoardRepository boards;
    private final BoardMemberRepository members;
    private final ListService listService;
    private final AppUserService userService;
    private final CardRepository cards;



    public BoardService(BoardRepository boards, BoardMemberRepository members, ListService listService, AppUserService userService, CardRepository cards) {
        this.boards = boards;
        this.members = members;
        this.listService = listService;
        this.userService = userService;
        this.cards = cards;
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
                .map(SecurityUtils::isGlobalAdmin)
                .orElse(false);

        if (!globalAdmin) {
            members.findByIdBoardIdAndIdUserId(boardId, userId)
                    .orElseThrow(() -> new IllegalStateException("Nemaš pristup ovom boardu."));
        }

        return boards.findById(boardId)
                .orElseThrow(() -> new IllegalStateException("Board ne postoji."));
    }

    public List<Board> listBoardsFor(User loggedUser) {
        boolean globalAdmin = SecurityUtils.isGlobalAdmin(loggedUser);
        return globalAdmin
                ? boards.findAllByArchivedAtIsNullOrderByIdDesc()
                : boards.findActiveBoardsForUser(loggedUser.getId());
    }

    public List<Board> listArchivedBoardsFor(User loggedUser) {
        boolean globalAdmin = SecurityUtils.isGlobalAdmin(loggedUser);
        return globalAdmin
                ? boards.findAllByArchivedAtIsNotNullOrderByArchivedAtDesc()
                : boards.findArchivedBoardsForUser(loggedUser.getId());
    }

    @Transactional
    public void archiveBoard(Long boardId, Long actorUserId) {
        Board b = boards.findById(boardId)
                .orElseThrow(() -> new IllegalStateException("Board ne postoji."));

        if (b.getArchivedAt() != null) {
            throw new IllegalStateException("Board je već zatvoren.");
        }

        boolean globalAdmin = userService.findById(actorUserId)
                .map(SecurityUtils::isGlobalAdmin)
                .orElse(false);

        if (!globalAdmin) {
            BoardMember bm = members.findByIdBoardIdAndIdUserId(boardId, actorUserId)
                    .orElseThrow(() -> new IllegalStateException("Nemaš pristup ovom boardu."));

            String role = bm.getRole();
            boolean canArchive = "OWNER".equals(role) || "ADMIN".equals(role);
            if (!canArchive) {
                throw new IllegalStateException("Samo OWNER/ADMIN može zatvoriti board.");
            }
        }

        Long doneListId = listService.requireLastListId(boardId);
        long open = cards.countNotInDone(boardId, doneListId);

        if (open > 0) {
            throw new IllegalStateException("Ne možeš zatvoriti board: ima " + open + " task(ova) van Done liste.");
        }

        b.setArchivedAt(LocalDateTime.now());
        boards.save(b);
    }

    @Transactional
    public void reopenBoard(Long boardId, Long actorUserId) {
        Board b = boards.findById(boardId)
                .orElseThrow(() -> new IllegalStateException("Board ne postoji."));

        if (b.getArchivedAt() == null) {
            throw new IllegalStateException("Board nije zatvoren.");
        }

        boolean globalAdmin = userService.findById(actorUserId)
                .map(carobnifrulas.web_tasks.security.model.SecurityUtils::isGlobalAdmin)
                .orElse(false);

        if (!globalAdmin) {
            BoardMember bm = members.findByIdBoardIdAndIdUserId(boardId, actorUserId)
                    .orElseThrow(() -> new IllegalStateException("Nemaš pristup ovom boardu."));

            if (!"OWNER".equals(bm.getRole())) {
                throw new IllegalStateException("Samo OWNER (ili global admin) može ponovo otvoriti board.");
            }
        }

        b.setArchivedAt(null);
        boards.save(b);
    }


}