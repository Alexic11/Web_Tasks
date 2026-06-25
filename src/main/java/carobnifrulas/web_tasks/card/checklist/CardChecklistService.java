package carobnifrulas.web_tasks.card.checklist;

import carobnifrulas.web_tasks.board.BoardMemberService;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.card.CardRealtimeBus;
import carobnifrulas.web_tasks.card.CardRepository;
import carobnifrulas.web_tasks.card.activity.CardActivityService;
import carobnifrulas.web_tasks.security.model.SecurityUtils;
import carobnifrulas.web_tasks.user.User;
import carobnifrulas.web_tasks.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CardChecklistService {

    private final CardChecklistRepository checklistRepository;
    private final CardRepository cardRepository;
    private final UserRepository users;
    private final BoardMemberService boardMemberService;
    private final CardActivityService activityService;

    public CardChecklistService(CardChecklistRepository checklistRepository,
                                CardRepository cardRepository,
                                UserRepository users,
                                BoardMemberService boardMemberService,
                                CardActivityService activityService) {
        this.checklistRepository = checklistRepository;
        this.cardRepository = cardRepository;
        this.users = users;
        this.boardMemberService = boardMemberService;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<CardChecklistItem> listForCard(Long cardId, Long viewerUserId) {
        Card card = requireCard(cardId);
        requireBoardAccess(card.getBoardId(), viewerUserId);
        return checklistRepository.findByCardIdOrderByPositionAscIdAsc(cardId);
    }

    @Transactional(readOnly = true)
    public ChecklistStats statsForCard(Long cardId) {
        long total = checklistRepository.countByCardId(cardId);
        long done = checklistRepository.countByCardIdAndDoneTrue(cardId);
        return new ChecklistStats(total, done);
    }

    @Transactional(readOnly = true)
    public Map<Long, ChecklistStats> statsForCards(Collection<Long> cardIds) {
        Map<Long, ChecklistStats> result = new HashMap<>();
        if (cardIds == null || cardIds.isEmpty()) {
            return result;
        }

        for (CardChecklistRepository.ChecklistStatsRow row : checklistRepository.findStatsByCardIds(cardIds)) {
            long total = row.getTotalCount() == null ? 0L : row.getTotalCount();
            long done = row.getDoneCount() == null ? 0L : row.getDoneCount();
            result.put(row.getCardId(), new ChecklistStats(total, done));
        }

        return result;
    }

    @Transactional
    public CardChecklistItem addItem(Long cardId, Long actorUserId, String title) {
        Card card = requireCard(cardId);
        requireWriteAccess(card.getBoardId(), actorUserId);

        String normalizedTitle = normalizeTitle(title);

        Integer maxPosition = checklistRepository.findMaxPosition(cardId);
        int nextPosition = maxPosition == null ? 1000 : maxPosition + 1000;

        CardChecklistItem item = new CardChecklistItem();
        item.setCardId(cardId);
        item.setTitle(normalizedTitle);
        item.setDone(false);
        item.setPosition(nextPosition);
        item.setCreatedBy(actorUserId);

        CardChecklistItem saved = checklistRepository.save(item);

        activityService.logChecklistAdded(cardId, actorUserId, saved.getTitle());
        CardRealtimeBus.publish(cardId, CardRealtimeBus.ChangeType.ALL);

        return saved;
    }

    @Transactional
    public CardChecklistItem setDone(Long itemId, Long actorUserId, boolean done) {
        CardChecklistItem item = requireItem(itemId);
        Card card = requireCard(item.getCardId());
        requireWriteAccess(card.getBoardId(), actorUserId);

        boolean oldDone = item.isDone();
        if (oldDone == done) {
            return item;
        }

        item.setDone(done);
        if (done) {
            item.setCompletedBy(actorUserId);
            item.setCompletedAt(LocalDateTime.now());
        } else {
            item.setCompletedBy(null);
            item.setCompletedAt(null);
        }

        CardChecklistItem saved = checklistRepository.save(item);

        activityService.logChecklistStatusChanged(card.getId(), actorUserId, saved.getTitle(), done);
        CardRealtimeBus.publish(card.getId(), CardRealtimeBus.ChangeType.ALL);

        return saved;
    }

    @Transactional
    public void deleteItem(Long itemId, Long actorUserId) {
        CardChecklistItem item = requireItem(itemId);
        Card card = requireCard(item.getCardId());
        requireWriteAccess(card.getBoardId(), actorUserId);

        String title = item.getTitle();
        checklistRepository.delete(item);

        activityService.logChecklistDeleted(card.getId(), actorUserId, title);
        CardRealtimeBus.publish(card.getId(), CardRealtimeBus.ChangeType.ALL);
    }

    private CardChecklistItem requireItem(Long itemId) {
        return checklistRepository.findById(itemId)
                .orElseThrow(() -> new IllegalStateException("Checklist stavka nije pronađena."));
    }

    private Card requireCard(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalStateException("Task nije pronađen."));
    }

    private User requireUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User ne postoji: " + userId));
    }

    private void requireBoardAccess(Long boardId, Long userId) {
        User user = requireUser(userId);
        if (SecurityUtils.isGlobalAdmin(user)) {
            return;
        }

        boardMemberService.getRole(boardId, userId);
    }

    private void requireWriteAccess(Long boardId, Long userId) {
        User user = requireUser(userId);
        if (SecurityUtils.isGlobalAdmin(user)) {
            return;
        }

        BoardRole role = boardMemberService.getRole(boardId, userId);
        if (role == BoardRole.VIEWER) {
            throw new IllegalStateException("Nemaš pravo izmjene checklist stavki.");
        }
    }

    private static String normalizeTitle(String title) {
        String t = title == null ? "" : title.trim();
        if (t.isBlank()) {
            throw new IllegalStateException("Unesi naziv checklist stavke.");
        }
        if (t.length() > 500) {
            throw new IllegalStateException("Checklist stavka je predugačka. Maksimalno 500 karaktera.");
        }
        return t;
    }

    public record ChecklistStats(long total, long done) {
        public boolean hasItems() {
            return total > 0;
        }

        public int percent() {
            if (total <= 0) {
                return 0;
            }
            return (int) Math.round((done * 100.0) / total);
        }
    }
}
