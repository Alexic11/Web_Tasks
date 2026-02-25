package carobnifrulas.web_tasks.board;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "board_members")
@Getter @Setter
public class BoardMember {

    @EmbeddedId
    private Id id;

    @Column(nullable = false, length = 20)
    private String role; // OWNER/ADMIN/MEMBER/VIEWER

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Embeddable
    @Getter @Setter
    public static class Id implements Serializable {
        @Column(name = "board_id")
        private Long boardId;

        @Column(name = "user_id")
        private Long userId;
    }
}