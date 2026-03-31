package carobnifrulas.web_tasks.card.comment;

import carobnifrulas.web_tasks.board.BoardMemberService;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.card.CardRealtimeBus;
import carobnifrulas.web_tasks.card.CardRepository;
import carobnifrulas.web_tasks.card.activity.CardActivityService;
import carobnifrulas.web_tasks.security.model.SecurityUtils;
import carobnifrulas.web_tasks.user.User;
import carobnifrulas.web_tasks.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CardCommentService {

    private final CardCommentRepository commentRepository;
    private final CardRepository cardRepository;
    private final UserRepository users;
    private final BoardMemberService boardMemberService;
    private final CardActivityService activityService;

    public CardCommentService(
            CardCommentRepository commentRepository,
            CardRepository cardRepository,
            UserRepository users,
            BoardMemberService boardMemberService,
            CardActivityService activityService
    ) {
        this.commentRepository = commentRepository;
        this.cardRepository = cardRepository;
        this.users = users;
        this.boardMemberService = boardMemberService;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<CardCommentRepository.CommentRow> listForCard(Long cardId, Long viewerUserId) {
        User viewer = requireUser(viewerUserId);

        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));

        if (SecurityUtils.isGlobalAdmin(viewer)) {
            return commentRepository.findRowsForCard(cardId);
        }

        BoardRole role = boardMemberService.getRole(card.getBoardId(), viewerUserId);
        if (role == null) {
            throw new SecurityException("Not a board member");
        }

        return commentRepository.findRowsForCard(cardId);
    }

    @Transactional
    public void addComment(Long cardId, Long actorUserId, String body) {
        User actor = requireUser(actorUserId);

        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Comment body is empty");
        }
        if (trimmed.length() > 5000) {
            throw new IllegalArgumentException("Comment too long (max 5000 chars)");
        }

        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));

        if (!SecurityUtils.isGlobalAdmin(actor)) {
            BoardRole role = boardMemberService.getRole(card.getBoardId(), actorUserId);
            boolean canWrite = role != null && role != BoardRole.VIEWER;
            if (!canWrite) {
                throw new SecurityException("No permission to comment");
            }
        }

        commentRepository.save(new CardComment(cardId, actorUserId, trimmed, null));

        activityService.logComment(cardId, actorUserId, trimmed);

        CardRealtimeBus.publish(cardId, CardRealtimeBus.ChangeType.ALL);
    }

    private User requireUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User ne postoji: " + userId));
    }
}