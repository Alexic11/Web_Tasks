package carobnifrulas.web_tasks.card.label;

import carobnifrulas.web_tasks.board.Board;
import carobnifrulas.web_tasks.board.BoardMemberService;
import carobnifrulas.web_tasks.board.BoardRepository;
import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.card.CardRepository;
import carobnifrulas.web_tasks.card.CardRealtimeBus;
import carobnifrulas.web_tasks.card.activity.CardActivityService;
import carobnifrulas.web_tasks.security.model.AppUserService;
import carobnifrulas.web_tasks.security.model.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CardLabelService {

    private final CardLabelRepository labels;
    private final CardLabelAssignmentRepository assignments;
    private final CardRepository cards;
    private final BoardRepository boards;
    private final BoardMemberService boardMemberService;
    private final AppUserService userService;
    private final CardActivityService activity;

    public CardLabelService(CardLabelRepository labels,
                            CardLabelAssignmentRepository assignments,
                            CardRepository cards,
                            BoardRepository boards,
                            BoardMemberService boardMemberService,
                            AppUserService userService,
                            CardActivityService activity) {
        this.labels = labels;
        this.assignments = assignments;
        this.cards = cards;
        this.boards = boards;
        this.boardMemberService = boardMemberService;
        this.userService = userService;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public List<CardLabel> listLabelsForBoard(Long boardId, Long actorUserId) {
        requireBoardAccess(boardId, actorUserId);
        return labels.findByBoardIdOrderByNameAsc(boardId);
    }

    @Transactional(readOnly = true)
    public List<CardLabel> listLabelsForCard(Long cardId, Long actorUserId) {
        Card c = requireCardAndAccess(cardId, actorUserId);
        return labels.findLabelsForCard(c.getId());
    }

    @Transactional(readOnly = true)
    public Map<Long, List<CardLabel>> labelsByCard(Collection<Long> cardIds, Long actorUserId) {
        Map<Long, List<CardLabel>> result = new LinkedHashMap<>();

        if (cardIds == null || cardIds.isEmpty()) {
            return result;
        }

        List<Long> ids = cardIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return result;
        }

        List<Card> found = cards.findAllById(ids);
        for (Card c : found) {
            requireBoardAccess(c.getBoardId(), actorUserId);
            result.put(c.getId(), new ArrayList<>());
        }

        for (CardLabelRepository.CardLabelRow r : labels.findLabelRowsForCards(ids)) {
            CardLabel l = new CardLabel();
            l.setId(r.getLabelId());
            l.setBoardId(r.getBoardId());
            l.setName(r.getName());
            l.setColor(r.getColor());

            result.computeIfAbsent(r.getCardId(), k -> new ArrayList<>()).add(l);
        }

        return result;
    }

    @Transactional
    public CardLabel createLabel(Long boardId, Long actorUserId, String name, String color) {
        requireBoardWritable(boardId, actorUserId);

        String n = normalizeName(name);
        String c = normalizeColor(color);

        if (labels.existsByBoardIdAndNameIgnoreCase(boardId, n)) {
            throw new IllegalStateException("Labela već postoji na ovom boardu: " + n);
        }

        CardLabel label = new CardLabel();
        label.setBoardId(boardId);
        label.setName(n);
        label.setColor(c);
        label.setCreatedBy(actorUserId);

        return labels.save(label);
    }

    @Transactional
    public void assignLabel(Long cardId, Long actorUserId, Long labelId) {
        Card c = requireCardWritable(cardId, actorUserId);
        CardLabel label = labels.findById(labelId)
                .orElseThrow(() -> new IllegalStateException("Labela ne postoji."));

        if (!label.getBoardId().equals(c.getBoardId())) {
            throw new IllegalStateException("Labela ne pripada ovom boardu.");
        }

        if (assignments.existsByIdCardIdAndIdLabelId(cardId, labelId)) {
            return;
        }

        CardLabelAssignment a = new CardLabelAssignment();
        a.setId(new CardLabelAssignmentId(cardId, labelId));
        a.setCreatedBy(actorUserId);
        assignments.save(a);

        activity.logLabelAssigned(cardId, actorUserId, label.getName());
        CardRealtimeBus.publish(cardId, CardRealtimeBus.ChangeType.ALL);
    }

    @Transactional
    public CardLabel createAndAssignLabel(Long cardId, Long actorUserId, String name, String color) {
        Card c = requireCardWritable(cardId, actorUserId);
        CardLabel label = createLabel(c.getBoardId(), actorUserId, name, color);
        assignLabel(cardId, actorUserId, label.getId());
        return label;
    }

    @Transactional
    public void removeLabel(Long cardId, Long actorUserId, Long labelId) {
        requireCardWritable(cardId, actorUserId);
        CardLabel label = labels.findById(labelId)
                .orElseThrow(() -> new IllegalStateException("Labela ne postoji."));

        if (!assignments.existsByIdCardIdAndIdLabelId(cardId, labelId)) {
            return;
        }

        assignments.deleteByIdCardIdAndIdLabelId(cardId, labelId);
        activity.logLabelRemoved(cardId, actorUserId, label.getName());
        CardRealtimeBus.publish(cardId, CardRealtimeBus.ChangeType.ALL);
    }

    @Transactional
    public void deleteLabel(Long labelId, Long actorUserId) {
        CardLabel label = labels.findById(labelId)
                .orElseThrow(() -> new IllegalStateException("Labela ne postoji."));

        requireBoardWritable(label.getBoardId(), actorUserId);
        assignments.deleteByIdLabelId(labelId);
        labels.delete(label);
    }

    private Card requireCardAndAccess(Long cardId, Long actorUserId) {
        Card c = cards.findById(cardId)
                .orElseThrow(() -> new IllegalStateException("Task ne postoji."));
        requireBoardAccess(c.getBoardId(), actorUserId);
        return c;
    }

    private Card requireCardWritable(Long cardId, Long actorUserId) {
        Card c = cards.findById(cardId)
                .orElseThrow(() -> new IllegalStateException("Task ne postoji."));
        requireBoardWritable(c.getBoardId(), actorUserId);
        return c;
    }

    private void requireBoardAccess(Long boardId, Long actorUserId) {
        if (isGlobalAdmin(actorUserId)) {
            boards.findById(boardId)
                    .orElseThrow(() -> new IllegalStateException("Board ne postoji."));
            return;
        }
        boardMemberService.getRole(boardId, actorUserId);
    }

    private void requireBoardWritable(Long boardId, Long actorUserId) {
        Board b = boards.findById(boardId)
                .orElseThrow(() -> new IllegalStateException("Board ne postoji."));

        if (b.getArchivedAt() != null) {
            throw new IllegalStateException("Board je zatvoren i nije dozvoljena izmjena labela.");
        }

        if (isGlobalAdmin(actorUserId)) {
            return;
        }

        if (!boardMemberService.canWrite(boardId, actorUserId)) {
            throw new IllegalStateException("Nemaš prava da mijenjaš labele na ovom boardu.");
        }
    }

    private boolean isGlobalAdmin(Long userId) {
        return userService.findById(userId)
                .map(SecurityUtils::isGlobalAdmin)
                .orElse(false);
    }

    private static String normalizeName(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) {
            throw new IllegalStateException("Unesi naziv labele.");
        }
        if (n.length() > 80) {
            throw new IllegalStateException("Naziv labele može imati najviše 80 karaktera.");
        }
        return n;
    }

    private static String normalizeColor(String color) {
        String c = color == null ? "BLUE" : color.trim().toUpperCase();
        return switch (c) {
            case "BLUE", "GREEN", "YELLOW", "RED", "PURPLE", "GRAY" -> c;
            default -> "BLUE";
        };
    }
}
