package carobnifrulas.web_tasks.card.comment;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_comments")
public class CardComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Lob
    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public CardComment() {}

    public CardComment(Long cardId, Long authorUserId, String body, LocalDateTime createdAt) {
        this.cardId = cardId;
        this.authorUserId = authorUserId;
        this.body = body;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getCardId() { return cardId; }
    public Long getAuthorUserId() { return authorUserId; }
    public String getBody() { return body; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setCardId(Long cardId) { this.cardId = cardId; }
    public void setAuthorUserId(Long authorUserId) { this.authorUserId = authorUserId; }
    public void setBody(String body) { this.body = body; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}