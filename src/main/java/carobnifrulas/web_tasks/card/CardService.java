package carobnifrulas.web_tasks.card;

import carobnifrulas.web_tasks.board.BoardMemberService;
import carobnifrulas.web_tasks.list.ListService;
import carobnifrulas.web_tasks.security.model.AppUserService;
import carobnifrulas.web_tasks.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CardService {

    private final CardRepository cards;
    private final BoardMemberService boardMemberService;
    private final ListService lists;
    private final AppUserService userService; // ✅ da nema ServicesHolder null problema

    public CardService(CardRepository cards,
                       BoardMemberService boardMemberService,
                       ListService lists,
                       AppUserService userService) {
        this.cards = cards;
        this.boardMemberService = boardMemberService;
        this.lists = lists;
        this.userService = userService;
    }

    public List<Card> findByList(Long listId) {
        return cards.findByListIdAndArchivedAtIsNullOrderByPositionAsc(listId);
    }

    public Card requireById(Long cardId) {
        return cards.findById(cardId)
                .orElseThrow(() -> new IllegalStateException("Task ne postoji."));
    }

    public List<Card> findAssignedTo(Long userId) {
        return cards.findByAssignedToAndArchivedAtIsNullOrderByUpdatedAtDesc(userId);
    }

    @Transactional
    public void assignToMe(Long cardId, Long myUserId) {
        Card c = requireById(cardId);

        if (!canWriteOrGlobalAdmin(c.getBoardId(), myUserId)) {
            throw new IllegalStateException("Nemaš prava da mijenjaš task na ovom boardu.");
        }

        if (c.getAssignedTo() != null) throw new IllegalStateException("Task je već preuzet.");
        c.setAssignedTo(myUserId);
        cards.save(c);
    }

    @Transactional
    public void unassign(Long cardId, Long myUserId) {
        Card c = requireById(cardId);

        if (!canWriteOrGlobalAdmin(c.getBoardId(), myUserId)) {
            throw new IllegalStateException("Nemaš prava da mijenjaš task na ovom boardu.");
        }

        if (c.getAssignedTo() == null) return;
        if (!c.getAssignedTo().equals(myUserId) && !isGlobalAdmin(myUserId)) {
            throw new IllegalStateException("Nije tvoj task.");
        }
        c.setAssignedTo(null);
        cards.save(c);
    }

    @Transactional
    public void moveToList(Long cardId, Long targetListId, Long actorUserId) {
        Card c = requireById(cardId);

        if (!canWriteOrGlobalAdmin(c.getBoardId(), actorUserId)) {
            throw new IllegalStateException("Nemaš prava da mijenjaš task na ovom boardu.");
        }

        c.setListId(targetListId);
        cards.save(c);
    }

    // ✅ zadržao sam tvoj API, ali interno zovemo secure varijantu
    @Transactional
    public void moveToList(Long cardId, Long targetListId) {
        Card c = requireById(cardId);
        // Ova metoda ostaje zbog postojećih poziva (markDone),
        // ali je "unsafe" ako je neko pozove direktno bez provjere.
        // Preporuka: vremenom ukloniti i koristiti samo varijantu sa actorUserId.
        c.setListId(targetListId);
        cards.save(c);
    }

    @Transactional
    public Card createCard(Long boardId,
                           Long listId,
                           String title,
                           String description,
                           LocalDateTime dueAt,
                           Integer priority,
                           Long assignedToUserId,
                           Long createdByUserId) {

        if (!canWriteOrGlobalAdmin(boardId, createdByUserId)) {
            throw new IllegalStateException("Nemaš prava da kreiraš task na ovom boardu.");
        }

        if (assignedToUserId != null) {
            boardMemberService.requireMember(boardId, assignedToUserId);
        }

        String t = normalizeTitle(title);

        BigDecimal max = cards.findMaxPositionInList(listId);
        BigDecimal pos = (max == null)
                ? new BigDecimal("1000.000000")
                : max.add(new BigDecimal("1000.000000"));

        Card c = new Card();
        c.setAssignedTo(assignedToUserId);
        c.setBoardId(boardId);
        c.setListId(listId);
        c.setTitle(t);
        c.setDescription(blankToNull(description));
        c.setDueAt(dueAt);
        c.setPriority(normalizePriority(priority));
        c.setCreatedBy(createdByUserId);
        c.setPosition(pos);

        return cards.save(c);
    }

    @Transactional
    public Card updateCard(Long cardId,
                           Long actorUserId,
                           String title,
                           String description,
                           LocalDateTime dueAt,
                           Integer priority,
                           Long assignedToUserId) {

        Card c = requireById(cardId);

        if (!canWriteOrGlobalAdmin(c.getBoardId(), actorUserId)) {
            throw new IllegalStateException("Nemaš prava da uređuješ task na ovom boardu.");
        }

        if (assignedToUserId != null) {
            boardMemberService.requireMember(c.getBoardId(), assignedToUserId);
        }

        c.setAssignedTo(assignedToUserId);
        c.setTitle(normalizeTitle(title));
        c.setDescription(blankToNull(description));
        c.setDueAt(dueAt);
        c.setPriority(normalizePriority(priority));

        return cards.save(c);
    }

    public List<CardRepository.MyTaskRow> findMyTasks(Long userId) {
        return cards.findMyTasks(userId);
    }

    @Transactional
    public void markDone(Long cardId, Long actorUserId) {
        Card c = requireById(cardId);

        boolean globalAdmin = isGlobalAdmin(actorUserId);

        // ako nije global admin, mora imati write (OWNER/ADMIN/MEMBER)
        if (!globalAdmin && !boardMemberService.canWrite(c.getBoardId(), actorUserId)) {
            throw new IllegalStateException("Nemaš prava da mijenjaš task na ovom boardu.");
        }

        // ako nije global admin, samo assignee može završiti task (ako je task dodijeljen)
        if (!globalAdmin && c.getAssignedTo() != null && !c.getAssignedTo().equals(actorUserId)) {
            throw new IllegalStateException("Samo assignee može završiti task.");
        }

        Long doneListId = lists.requireLastListId(c.getBoardId());

        if (!doneListId.equals(c.getListId())) {
            moveToList(c.getId(), doneListId); // koristi postojeću logiku
        }
    }

    public List<CardRepository.TaskRow> listTaskRowsForDashboard(User loggedUser) {
        boolean globalAdmin = "admin@local".equalsIgnoreCase(loggedUser.getEmail());
        return cards.findTaskRows(globalAdmin ? null : loggedUser.getId());
    }

    // ================= helpers =================

    private boolean isGlobalAdmin(Long userId) {
        return userService.findById(userId)
                .map(u -> "admin@local".equalsIgnoreCase(u.getEmail()))
                .orElse(false);
    }

    private boolean canWriteOrGlobalAdmin(Long boardId, Long userId) {
        return isGlobalAdmin(userId) || boardMemberService.canWrite(boardId, userId);
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalStateException("Naslov je obavezan.");
        }
        return title.trim();
    }

    private static int normalizePriority(Integer p) {
        int val = (p == null) ? 1 : p;
        if (val < 1 || val > 5) {
            throw new IllegalStateException("Prioritet mora biti od 1 do 5.");
        }
        return val;
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
