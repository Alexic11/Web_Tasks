package carobnifrulas.web_tasks.list;

import carobnifrulas.web_tasks.board.BoardMemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ListService {

    private final ListRepository lists;
    private final BoardMemberService boardMemberService;

    public ListService(ListRepository lists, BoardMemberService boardMemberService) {
        this.lists = lists;
        this.boardMemberService = boardMemberService;
    }

    public List<ListEntity> findByBoard(Long boardId) {
        return lists.findByBoardIdOrderByPositionAsc(boardId);
    }

    @Transactional
    public ListEntity createList(Long boardId, Long actorUserId, String title) {
        if (!boardMemberService.canWrite(boardId, actorUserId)) {
            throw new IllegalStateException("Nemaš prava da dodaješ liste na ovom boardu.");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalStateException("Naziv liste je obavezan.");
        }

        BigDecimal max = lists.findMaxPositionInBoard(boardId);
        BigDecimal pos = (max == null) ? new BigDecimal("1000.000000") : max.add(new BigDecimal("1000.000000"));

        ListEntity l = new ListEntity();
        l.setBoardId(boardId);
        l.setTitle(title.trim());
        l.setPosition(pos);

        return lists.save(l);
    }

    @Transactional
    public void createDefaultListsIfMissing(Long boardId, Long actorUserId) {
        if (!boardMemberService.canWrite(boardId, actorUserId)) {
            throw new IllegalStateException("Nemaš prava da kreiraš liste na ovom boardu.");
        }
        if (lists.existsByBoardId(boardId)) {
            return; // već postoje liste
        }
        createList(boardId, actorUserId, "To do");
        createList(boardId, actorUserId, "Doing");
        createList(boardId, actorUserId, "Done");
    }

    public ListEntity requireListOnBoard(Long boardId, Long listId) {
        if (boardId == null) {
            throw new IllegalStateException("Board je obavezan.");
        }
        if (listId == null) {
            throw new IllegalStateException("Lista je obavezna.");
        }

        ListEntity list = lists.findById(listId)
                .orElseThrow(() -> new IllegalStateException("Lista ne postoji."));

        if (!boardId.equals(list.getBoardId())) {
            throw new IllegalStateException("Lista ne pripada ovom boardu.");
        }

        return list;
    }

    public Long requireLastListId(Long boardId) {
        return lists.findFirstByBoardIdOrderByPositionDesc(boardId)
                .orElseThrow(() -> new IllegalStateException("Board nema nijednu listu."))
                .getId();
    }

}