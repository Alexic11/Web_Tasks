package carobnifrulas.web_tasks.card;

import carobnifrulas.web_tasks.board.BoardMemberService;
import carobnifrulas.web_tasks.board.BoardRealtimeBus;
import carobnifrulas.web_tasks.card.activity.CardActivityService;
import carobnifrulas.web_tasks.list.ListService;
import carobnifrulas.web_tasks.notification.NotificationService;
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
    private final AppUserService userService;
    private final CardActivityService activity;
    private final NotificationService notificationService;

    public CardService(CardRepository cards,
                       BoardMemberService boardMemberService,
                       ListService lists,
                       AppUserService userService,
                       CardActivityService activity,
                       NotificationService notificationService) {
        this.cards = cards;
        this.boardMemberService = boardMemberService;
        this.lists = lists;
        this.userService = userService;
        this.activity = activity;
        this.notificationService = notificationService;
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
        if (c.getAssignedTo() != null) {
            throw new IllegalStateException("Task je već preuzet.");
        }

        c.setAssignedTo(myUserId);
        cards.save(c);

        activity.logAssign(c.getId(), myUserId, myUserId);
        BoardRealtimeBus.publish(c.getBoardId(), BoardRealtimeBus.ChangeType.CARD_CHANGED);

        // Ne šaljemo notifikaciju samom sebi kad klikne "Preuzmi"
    }

    @Transactional
    public void unassign(Long cardId, Long myUserId) {
        Card c = requireById(cardId);

        if (!canWriteOrGlobalAdmin(c.getBoardId(), myUserId)) {
            throw new IllegalStateException("Nemaš prava da mijenjaš task na ovom boardu.");
        }

        if (c.getAssignedTo() == null) return;

        Long oldAssignee = c.getAssignedTo();

        if (!oldAssignee.equals(myUserId) && !isGlobalAdmin(myUserId)) {
            throw new IllegalStateException("Nije tvoj task.");
        }

        c.setAssignedTo(null);
        cards.save(c);

        activity.logUnassign(c.getId(), myUserId, oldAssignee);
        BoardRealtimeBus.publish(c.getBoardId(), BoardRealtimeBus.ChangeType.CARD_CHANGED);
    }

    @Transactional
    public void moveToList(Long cardId, Long targetListId, Long actorUserId) {
        Card c = requireById(cardId);

        if (!canWriteOrGlobalAdmin(c.getBoardId(), actorUserId)) {
            throw new IllegalStateException("Nemaš prava da mijenjaš task na ovom boardu.");
        }

        Long fromListId = c.getListId();
        if (fromListId != null && fromListId.equals(targetListId)) return;

        c.setListId(targetListId);
        cards.save(c);

        activity.logMoveList(c.getId(), actorUserId, fromListId, targetListId);
        BoardRealtimeBus.publish(c.getBoardId(), BoardRealtimeBus.ChangeType.CARD_MOVED);
    }

    @Transactional
    public void moveToList(Long cardId, Long targetListId) {
        Card c = requireById(cardId);
        c.setListId(targetListId);
        cards.save(c);
        BoardRealtimeBus.publish(c.getBoardId(), BoardRealtimeBus.ChangeType.CARD_MOVED);
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

        Card saved = cards.save(c);

        activity.logCreated(saved.getId(), createdByUserId, saved.getTitle());

        if (saved.getAssignedTo() != null) {
            activity.logAssign(saved.getId(), createdByUserId, saved.getAssignedTo());

            userService.findById(saved.getAssignedTo()).ifPresent(recipient -> {
                User actor = userService.findById(createdByUserId).orElse(null);
                notificationService.createTaskAssignedNotification(recipient, actor, saved);
            });
        }

        if (saved.getDueAt() != null) {
            activity.logDueChange(saved.getId(), createdByUserId, null, saved.getDueAt());
        }

        if (saved.getPriority() != null && saved.getPriority() != 1) {
            activity.logPriorityChange(saved.getId(), createdByUserId, 1, saved.getPriority());
        }

        BoardRealtimeBus.publish(saved.getBoardId(), BoardRealtimeBus.ChangeType.CARD_CHANGED);

        return saved;
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

        Integer oldPr = c.getPriority();
        LocalDateTime oldDue = c.getDueAt();
        Long oldAssignee = c.getAssignedTo();
        String oldTitle = c.getTitle();
        String oldDesc = c.getDescription();

        c.setAssignedTo(assignedToUserId);
        c.setTitle(normalizeTitle(title));
        c.setDescription(blankToNull(description));
        c.setDueAt(dueAt);
        c.setPriority(normalizePriority(priority));

        Card saved = cards.save(c);

        if (!eqInt(oldPr, saved.getPriority())) {
            activity.logPriorityChange(saved.getId(), actorUserId, oldPr, saved.getPriority());
        }

        if (!eqDt(oldDue, saved.getDueAt())) {
            activity.logDueChange(saved.getId(), actorUserId, oldDue, saved.getDueAt());
        }

        if (!eqLong(oldAssignee, saved.getAssignedTo())) {
            if (saved.getAssignedTo() == null) {
                activity.logUnassign(saved.getId(), actorUserId, oldAssignee);
            } else {
                activity.logAssign(saved.getId(), actorUserId, saved.getAssignedTo());

                userService.findById(saved.getAssignedTo()).ifPresent(recipient -> {
                    User actor = userService.findById(actorUserId).orElse(null);

                    if (oldAssignee == null) {
                        notificationService.createTaskAssignedNotification(recipient, actor, saved);
                    } else {
                        notificationService.createTaskReassignedNotification(recipient, actor, saved);
                    }
                });
            }
        }

        boolean titleChanged = !safeStr(oldTitle).equals(safeStr(saved.getTitle()));
        boolean descChanged = !safeStr(oldDesc).equals(safeStr(saved.getDescription()));
        if (titleChanged || descChanged) {
            activity.logUpdated(saved.getId(), actorUserId, "Promijenjeni detalji (naslov/opis).");
        }

        BoardRealtimeBus.publish(saved.getBoardId(), BoardRealtimeBus.ChangeType.CARD_CHANGED);

        return saved;
    }

    @Transactional
    public void markDone(Long cardId, Long actorUserId) {
        Card c = requireById(cardId);

        boolean globalAdmin = isGlobalAdmin(actorUserId);

        if (!globalAdmin && !boardMemberService.canWrite(c.getBoardId(), actorUserId)) {
            throw new IllegalStateException("Nemaš prava da mijenjaš task na ovom boardu.");
        }

        if (!globalAdmin && c.getAssignedTo() != null && !c.getAssignedTo().equals(actorUserId)) {
            throw new IllegalStateException("Samo assignee može završiti task.");
        }

        Long doneListId = lists.requireLastListId(c.getBoardId());
        Long fromListId = c.getListId();

        if (!doneListId.equals(fromListId)) {
            c.setListId(doneListId);
            cards.save(c);
            activity.logDone(c.getId(), actorUserId, fromListId, doneListId);
            BoardRealtimeBus.publish(c.getBoardId(), BoardRealtimeBus.ChangeType.CARD_MOVED);
        }
    }

    public List<CardRepository.MyTaskRow> findMyTasks(Long userId) {
        return cards.findMyTasks(userId);
    }

    public List<CardRepository.TaskRow> listTaskRowsForDashboard(User loggedUser) {
        boolean globalAdmin = "admin@local".equalsIgnoreCase(loggedUser.getEmail());
        return cards.findTaskRows(globalAdmin ? null : loggedUser.getId());
    }

    @Transactional
    public void reorderWithinList(Long cardId, Long listId, int targetIndex, Long actorUserId) {
        Card moving = requireById(cardId);

        if (!canWriteOrGlobalAdmin(moving.getBoardId(), actorUserId)) {
            throw new IllegalStateException("Nemaš prava da mijenjaš task na ovom boardu.");
        }

        Long oldListId = moving.getListId();
        BigDecimal oldPos = moving.getPosition();

        List<Card> items = cards.findByListIdAndArchivedAtIsNullOrderByPositionAsc(listId);
        items.removeIf(c2 -> c2.getId().equals(cardId));

        if (targetIndex < 0) targetIndex = 0;
        if (targetIndex > items.size()) targetIndex = items.size();

        BigDecimal newPos;
        if (items.isEmpty()) {
            newPos = new BigDecimal("1000.000000");
        } else if (targetIndex == 0) {
            newPos = items.get(0).getPosition().subtract(new BigDecimal("1000.000000"));
        } else if (targetIndex == items.size()) {
            newPos = items.get(items.size() - 1).getPosition().add(new BigDecimal("1000.000000"));
        } else {
            BigDecimal prev = items.get(targetIndex - 1).getPosition();
            BigDecimal next = items.get(targetIndex).getPosition();
            newPos = prev.add(next).divide(new BigDecimal("2"), java.math.RoundingMode.HALF_UP);

            if (newPos.equals(prev) || newPos.equals(next)) {
                reindexList(listId);
                items = cards.findByListIdAndArchivedAtIsNullOrderByPositionAsc(listId);
                items.removeIf(c2 -> c2.getId().equals(cardId));

                if (targetIndex == 0) {
                    newPos = items.get(0).getPosition().subtract(new BigDecimal("1000.000000"));
                } else if (targetIndex == items.size()) {
                    newPos = items.get(items.size() - 1).getPosition().add(new BigDecimal("1000.000000"));
                } else {
                    BigDecimal p2 = items.get(targetIndex - 1).getPosition();
                    BigDecimal n2 = items.get(targetIndex).getPosition();
                    newPos = p2.add(n2).divide(new BigDecimal("2"), java.math.RoundingMode.HALF_UP);
                }
            }
        }

        moving.setListId(listId);
        moving.setPosition(newPos);
        cards.save(moving);

        if (oldListId != null && !oldListId.equals(listId)) {
            activity.logMoveList(moving.getId(), actorUserId, oldListId, listId);
        } else {
            activity.logReorder(moving.getId(), actorUserId, oldPos, newPos);
        }

        BoardRealtimeBus.publish(moving.getBoardId(), BoardRealtimeBus.ChangeType.CARD_MOVED);
    }

    @Transactional
    public void reindexList(Long listId) {
        List<Card> items = cards.findByListIdAndArchivedAtIsNullOrderByPositionAsc(listId);
        BigDecimal pos = new BigDecimal("1000.000000");
        for (Card c : items) {
            c.setPosition(pos);
            pos = pos.add(new BigDecimal("1000.000000"));
        }
        cards.saveAll(items);
    }

    public long countOpenTasks(Long boardId) {
        Long doneListId = lists.requireLastListId(boardId);
        return cards.countOpenInBoard(boardId, doneListId);
    }

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

    private static boolean eqInt(Integer a, Integer b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean eqLong(Long a, Long b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean eqDt(LocalDateTime a, LocalDateTime b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String safeStr(String s) {
        return s == null ? "" : s;
    }
}