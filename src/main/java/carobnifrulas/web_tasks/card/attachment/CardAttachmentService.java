package carobnifrulas.web_tasks.card.attachment;

import carobnifrulas.web_tasks.board.BoardMemberRepository;
import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.card.CardRealtimeBus;
import carobnifrulas.web_tasks.card.CardRepository;
import carobnifrulas.web_tasks.card.activity.CardActivityService;
import carobnifrulas.web_tasks.security.model.SecurityUtils;
import carobnifrulas.web_tasks.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class CardAttachmentService {

    private final CardAttachmentRepository attachmentRepository;
    private final CardRepository cardRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final AttachmentStorageService storageService;
    private final CardActivityService activityService;

    public CardAttachmentService(CardAttachmentRepository attachmentRepository,
                                 CardRepository cardRepository,
                                 BoardMemberRepository boardMemberRepository,
                                 AttachmentStorageService storageService,
                                 CardActivityService activityService) {
        this.attachmentRepository = attachmentRepository;
        this.cardRepository = cardRepository;
        this.boardMemberRepository = boardMemberRepository;
        this.storageService = storageService;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<CardAttachment> findByCard(Long cardId, User user) {
        Card card = requireCard(cardId);
        requireBoardAccess(card.getBoardId(), user);
        return attachmentRepository.findByCardIdOrderByCreatedAtAscIdAsc(cardId);
    }

    @Transactional
    public CardAttachment upload(Long cardId,
                                 User user,
                                 String originalFilename,
                                 String contentType,
                                 InputStream inputStream) {
        Card card = requireCard(cardId);
        requireWriteAccess(card.getBoardId(), user);

        AttachmentStorageService.StoredAttachment stored =
                storageService.save(cardId, originalFilename, contentType, inputStream);

        CardAttachment attachment = new CardAttachment();
        attachment.setCardId(cardId);
        attachment.setUploadedBy(user.getId());
        attachment.setOriginalFilename(stored.originalFilename());
        attachment.setStoredFilename(stored.storedFilename());
        attachment.setContentType(stored.contentType());
        attachment.setSizeBytes(stored.sizeBytes());

        CardAttachment saved = attachmentRepository.save(attachment);

        activityService.logUpdated(
                cardId,
                user.getId(),
                "Dodan attachment: " + saved.getOriginalFilename()
        );

        CardRealtimeBus.publish(cardId, CardRealtimeBus.ChangeType.ALL);

        return saved;
    }

    @Transactional(readOnly = true)
    public DownloadableAttachment getDownloadable(Long attachmentId, User user) {
        CardAttachment attachment = requireAttachment(attachmentId);
        Card card = requireCard(attachment.getCardId());
        requireBoardAccess(card.getBoardId(), user);

        Path path = storageService.loadAsPath(attachment.getCardId(), attachment.getStoredFilename());
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new IllegalStateException("Attachment fajl ne postoji na disku.");
        }

        return new DownloadableAttachment(attachment, path);
    }

    @Transactional
    public void delete(Long attachmentId, User user) {
        CardAttachment attachment = requireAttachment(attachmentId);
        Card card = requireCard(attachment.getCardId());
        requireWriteAccess(card.getBoardId(), user);

        String fileName = attachment.getOriginalFilename();

        attachmentRepository.delete(attachment);
        storageService.delete(attachment.getCardId(), attachment.getStoredFilename());

        activityService.logUpdated(
                card.getId(),
                user.getId(),
                "Obrisan attachment: " + fileName
        );

        CardRealtimeBus.publish(card.getId(), CardRealtimeBus.ChangeType.ALL);
    }

    private CardAttachment requireAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment nije pronađen. ID=" + attachmentId));
    }

    private Card requireCard(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Task nije pronađen. ID=" + cardId));
    }

    private void requireBoardAccess(Long boardId, User user) {
        if (user == null) {
            throw new IllegalStateException("Korisnik nije prijavljen.");
        }

        if (isGlobalAdmin(user)) {
            return;
        }

        boolean member = boardMemberRepository.existsByIdBoardIdAndIdUserId(boardId, user.getId());
        if (!member) {
            throw new IllegalStateException("Nemate pristup ovom tasku.");
        }
    }

    private void requireWriteAccess(Long boardId, User user) {
        if (user == null) {
            throw new IllegalStateException("Korisnik nije prijavljen.");
        }

        if (isGlobalAdmin(user)) {
            return;
        }

        String role = boardMemberRepository.findByIdBoardIdAndIdUserId(boardId, user.getId())
                .map(member -> member.getRole())
                .orElseThrow(() -> new IllegalStateException("Nemate pristup ovom tasku."));

        if ("VIEWER".equalsIgnoreCase(role)) {
            throw new IllegalStateException("Nemate dozvolu za izmjenu attachmenta.");
        }
    }

    private boolean isGlobalAdmin(User user) {
        return SecurityUtils.isGlobalAdmin(user);
    }

    public record DownloadableAttachment(CardAttachment attachment, Path path) {
    }
}