package carobnifrulas.web_tasks.board;

import carobnifrulas.web_tasks.user.User;
import carobnifrulas.web_tasks.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
public class BoardMemberService {

    private final BoardMemberRepository members;
    private final UserRepository users;

    public BoardMemberService(BoardMemberRepository members, UserRepository users) {
        this.members = members;
        this.users = users;
    }

    public BoardRole getRole(Long boardId, Long userId) {
        return members.findByIdBoardIdAndIdUserId(boardId, userId)
                .map(bm -> BoardRole.valueOf(bm.getRole()))
                .orElseThrow(() -> new IllegalStateException("User is not member of board."));
    }

    public boolean isMember(Long boardId, Long userId) {
        return members.existsByIdBoardIdAndIdUserId(boardId, userId);
    }

    public boolean canWrite(Long boardId, Long userId) {
        BoardRole r = getRole(boardId, userId);
        return r == BoardRole.OWNER || r == BoardRole.ADMIN || r == BoardRole.MEMBER;
    }

    public boolean canManageMembers(Long boardId, Long userId) {
        BoardRole r = getRole(boardId, userId);
        return r == BoardRole.OWNER || r == BoardRole.ADMIN;
    }

    public List<BoardMemberRepository.MemberRow> listMemberRows(Long boardId) {
        return members.findMemberRows(boardId);
    }

    @Transactional
    public void addMember(Long boardId, Long actorUserId, String email, BoardRole role) {
        requireManageMembers(boardId, actorUserId);

        if (role == BoardRole.OWNER) {
            throw new IllegalStateException("Ne možeš dodati OWNER preko UI.");
        }

        User u = users.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalStateException("Korisnik ne postoji: " + email));

        if (members.existsByIdBoardIdAndIdUserId(boardId, u.getId())) {
            throw new IllegalStateException("Korisnik je već član boarda.");
        }

        BoardMember bm = new BoardMember();
        bm.setId(new BoardMemberId(boardId, u.getId()));
        bm.setRole(role.name());
        members.save(bm);
    }

    @Transactional
    public void changeRole(Long boardId, Long actorUserId, Long targetUserId, BoardRole newRole) {
        requireManageMembers(boardId, actorUserId);

        if (newRole == BoardRole.OWNER) {
            throw new IllegalStateException("Ne možeš dodijeliti OWNER ovdje.");
        }

        BoardMember bm = members.findByIdBoardIdAndIdUserId(boardId, targetUserId)
                .orElseThrow(() -> new IllegalStateException("Član nije pronađen."));

        if (BoardRole.valueOf(bm.getRole()) == BoardRole.OWNER) {
            throw new IllegalStateException("Ne možeš mijenjati rolu OWNER-u.");
        }

        bm.setRole(newRole.name());
        members.save(bm);
    }

    @Transactional
    public void removeMember(Long boardId, Long actorUserId, Long targetUserId) {
        requireManageMembers(boardId, actorUserId);

        BoardMember bm = members.findByIdBoardIdAndIdUserId(boardId, targetUserId)
                .orElseThrow(() -> new IllegalStateException("Član nije pronađen."));

        if (BoardRole.valueOf(bm.getRole()) == BoardRole.OWNER) {
            throw new IllegalStateException("Ne možeš ukloniti OWNER-a.");
        }

        members.delete(bm);
    }

    private void requireManageMembers(Long boardId, Long actorUserId) {
        if (!canManageMembers(boardId, actorUserId)) {
            throw new IllegalStateException("Nemaš prava da upravljaš članovima boarda.");
        }
    }

    public List<BoardMemberRepository.AssigneeRow> listAssignees(Long boardId) {
        return members.findAssignees(boardId);
    }

    public void requireMember(Long boardId, Long userId) {
        members.findByIdBoardIdAndIdUserId(boardId, userId)
                .orElseThrow(() -> new IllegalStateException("Korisnik nije član ovog boarda."));
    }
}