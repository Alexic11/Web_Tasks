package carobnifrulas.web_tasks.card.activity;

import carobnifrulas.web_tasks.list.ListEntity;
import carobnifrulas.web_tasks.list.ListRepository;
import carobnifrulas.web_tasks.user.User;
import carobnifrulas.web_tasks.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class CardActivityService {

    public static final String CREATED = "CREATED";
    public static final String MOVED_LIST = "MOVED_LIST";
    public static final String PRIORITY_CHANGED = "PRIORITY_CHANGED";
    public static final String DUE_CHANGED = "DUE_CHANGED";
    public static final String ASSIGNED = "ASSIGNED";
    public static final String UNASSIGNED = "UNASSIGNED";
    public static final String DONE = "DONE";
    public static final String UPDATED = "UPDATED";
    public static final String COMMENTED = "COMMENTED";
    public static final String CHECKLIST_ADDED = "CHECKLIST_ADDED";
    public static final String CHECKLIST_DONE = "CHECKLIST_DONE";
    public static final String CHECKLIST_REOPENED = "CHECKLIST_REOPENED";
    public static final String CHECKLIST_DELETED = "CHECKLIST_DELETED";

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final CardActivityRepository repo;
    private final UserRepository users;
    private final ListRepository lists;

    public CardActivityService(CardActivityRepository repo, UserRepository users, ListRepository lists) {
        this.repo = repo;
        this.users = users;
        this.lists = lists;
    }

    @Transactional
    public void logCreated(Long cardId, Long actorUserId, String title) {
        log(cardId, actorUserId, CREATED, null, safe("Task: " + title));
    }

    @Transactional
    public void logMoveList(Long cardId, Long actorUserId, Long fromListId, Long toListId) {
        String from = listLabel(fromListId);
        String to = listLabel(toListId);
        log(cardId, actorUserId, MOVED_LIST, from, to);
    }

    @Transactional
    public void logDone(Long cardId, Long actorUserId, Long fromListId, Long toListId) {
        String from = listLabel(fromListId);
        String to = listLabel(toListId);
        log(cardId, actorUserId, DONE, from, to);
    }

    @Transactional
    public void logPriorityChange(Long cardId, Long actorUserId, Integer oldP, Integer newP) {
        log(cardId, actorUserId, PRIORITY_CHANGED, "P" + safePr(oldP), "P" + safePr(newP));
    }

    @Transactional
    public void logDueChange(Long cardId, Long actorUserId, LocalDateTime oldDue, LocalDateTime newDue) {
        log(cardId, actorUserId, DUE_CHANGED, dueLabel(oldDue), dueLabel(newDue));
    }

    @Transactional
    public void logAssign(Long cardId, Long actorUserId, Long newAssigneeId) {
        log(cardId, actorUserId, ASSIGNED, null, userEmailOrDash(newAssigneeId));
    }

    @Transactional
    public void logUnassign(Long cardId, Long actorUserId, Long oldAssigneeId) {
        log(cardId, actorUserId, UNASSIGNED, userEmailOrDash(oldAssigneeId), null);
    }

    @Transactional
    public void logUpdated(Long cardId, Long actorUserId, String summary) {
        // za "general update" kad se promijeni nešto drugo (npr naslov/opis)
        log(cardId, actorUserId, UPDATED, null, safe(summary));
    }
    @Transactional
    public void logComment(Long cardId, Long actorUserId, String body) {
        String preview = safe(body).trim();
        if (preview.length() > 120) preview = preview.substring(0, 120) + "…";
        log(cardId, actorUserId, COMMENTED, null, preview);
    }

    @Transactional
    public void logChecklistAdded(Long cardId, Long actorUserId, String title) {
        log(cardId, actorUserId, CHECKLIST_ADDED, null, preview(title));
    }

    @Transactional
    public void logChecklistStatusChanged(Long cardId, Long actorUserId, String title, boolean done) {
        log(cardId, actorUserId, done ? CHECKLIST_DONE : CHECKLIST_REOPENED,
                preview(title),
                done ? "DONE" : "OPEN");
    }

    @Transactional
    public void logChecklistDeleted(Long cardId, Long actorUserId, String title) {
        log(cardId, actorUserId, CHECKLIST_DELETED, preview(title), null);
    }

    private void log(Long cardId, Long actorUserId, String action, String oldValue, String newValue) {
        CardActivity a = new CardActivity();
        a.setCardId(cardId);
        a.setActorUserId(actorUserId);
        a.setActorEmail(requireEmail(actorUserId));
        a.setAction(action);
        a.setOldValue(oldValue);
        a.setNewValue(newValue);
        repo.save(a);
    }

    private String requireEmail(Long userId) {
        return users.findById(userId)
                .map(User::getEmail)
                .orElseThrow(() -> new IllegalStateException("User ne postoji: " + userId));
    }

    private String userEmailOrDash(Long userId) {
        if (userId == null) return "—";
        return users.findById(userId).map(User::getEmail).orElse(String.valueOf(userId));
    }

    private String listLabel(Long listId) {
        if (listId == null) return "—";
        return lists.findById(listId)
                .map(ListEntity::getTitle)
                .orElse("list#" + listId);
    }

    private static int safePr(Integer p) {
        return p == null ? 1 : p;
    }

    private static String dueLabel(LocalDateTime dt) {
        return dt == null ? "—" : DTF.format(dt);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String preview(String s) {
        String p = safe(s).trim();
        if (p.length() > 120) {
            p = p.substring(0, 120) + "…";
        }
        return p;
    }

    @Transactional
    public void logReorder(Long cardId, Long actorUserId, BigDecimal oldPos, BigDecimal newPos) {
        log(cardId, actorUserId, "ORDER_CHANGED",
                oldPos == null ? "—" : oldPos.toPlainString(),
                newPos == null ? "—" : newPos.toPlainString());
    }


    @Transactional(readOnly = true)
    public java.util.List<CardActivity> listForCard(Long cardId) {
        return repo.findByCardIdOrderByCreatedAtDesc(cardId);
    }


}
