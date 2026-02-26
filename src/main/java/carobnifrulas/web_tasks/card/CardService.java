package carobnifrulas.web_tasks.card;

import carobnifrulas.web_tasks.board.BoardMemberService;
import carobnifrulas.web_tasks.list.ListService;
import carobnifrulas.web_tasks.services.ServicesHolder;
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

    protected ServicesHolder services;



    public CardService(CardRepository cards, BoardMemberService boardMemberService, ListService lists) {
        this.cards = cards;
        this.boardMemberService = boardMemberService;
        this.lists = lists;
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
        Card c = cards.findById(cardId).orElseThrow();
        if (c.getAssignedTo() != null) throw new IllegalStateException("Task je već preuzet.");
        c.setAssignedTo(myUserId);
        cards.save(c);
    }

    @Transactional
    public void unassign(Long cardId, Long myUserId) {
        Card c = cards.findById(cardId).orElseThrow();
        if (c.getAssignedTo() == null) return;
        if (!c.getAssignedTo().equals(myUserId)) throw new IllegalStateException("Nije tvoj task.");
        c.setAssignedTo(null);
        cards.save(c);
    }

    @Transactional
    public void moveToList(Long cardId, Long targetListId) {
        Card c = cards.findById(cardId).orElseThrow();
        c.setListId(targetListId);
        cards.save(c);
    }

    @Transactional
    public Card createCard(Long boardId,
                           Long listId,
                           String title,
                           String description,
                           LocalDateTime dueAt,
                           Long assignedToUserId,   // ✅ NEW (nullable)
                           Long createdByUserId) {

        // server-side security
        if (!boardMemberService.canWrite(boardId, createdByUserId)) {
            throw new IllegalStateException("Nemaš prava da kreiraš task na ovom boardu.");
        }

        if (assignedToUserId != null) {
            // mora biti član boarda, i mi moramo imati write
            boardMemberService.requireMember(boardId, assignedToUserId); // ili tvoja metoda
        }

        String t = normalizeTitle(title);

        BigDecimal max = cards.findMaxPositionInList(listId);
        BigDecimal pos = (max == null) ? new BigDecimal("1000.000000") : max.add(new BigDecimal("1000.000000"));

        Card c = new Card();

        c.setAssignedTo(assignedToUserId);
        c.setBoardId(boardId);
        c.setListId(listId);
        c.setTitle(t);
        c.setDescription(blankToNull(description));
        c.setDueAt(dueAt);              // ✅ opciono (može null)
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
                           Long assignedToUserId)  {

        Card c = requireById(cardId);

        // server-side security: gledamo board preko kartice
        if (!boardMemberService.canWrite(c.getBoardId(), actorUserId)) {
            throw new IllegalStateException("Nemaš prava da uređuješ task na ovom boardu.");
        }



        if (assignedToUserId != null) {
            boardMemberService.requireMember(c.getBoardId(), assignedToUserId);
        }
        c.setAssignedTo(assignedToUserId);
        c.setTitle(normalizeTitle(title));
        c.setDescription(blankToNull(description));
        c.setDueAt(dueAt);              // ✅ opciono

        return cards.save(c);
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalStateException("Naslov je obavezan.");
        }
        return title.trim();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public List<CardRepository.MyTaskRow> findMyTasks(Long userId) {
        return cards.findMyTasks(userId);
    }

    @Transactional
    public void markDone(Long cardId, Long actorUserId) {
        Card c = requireById(cardId);

        boolean globalAdmin = "admin@local".equalsIgnoreCase(
                services.userService.findById(actorUserId)
                        .map(User::getEmail)
                        .orElse("")
        );

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
            moveToList(c.getId(), doneListId); // koristi tvoju postojeću logiku
        }
    }


    public List<CardRepository.TaskRow> listTaskRowsForDashboard(User loggedUser) {
        boolean globalAdmin = carobnifrulas.web_tasks.security.model.SecurityUtils.isGlobalAdmin(loggedUser);
        return cards.findTaskRows(globalAdmin ? null : loggedUser.getId());
    }


}