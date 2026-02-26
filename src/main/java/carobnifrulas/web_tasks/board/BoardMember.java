package carobnifrulas.web_tasks.board;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "board_members")
public class BoardMember {

    @EmbeddedId
    private BoardMemberId id;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "joined_at", insertable = false, updatable = false)
    private Instant joinedAt;

    public BoardMemberId getId() {
        return id;
    }

    public void setId(BoardMemberId id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}