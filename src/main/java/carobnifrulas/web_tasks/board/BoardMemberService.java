package carobnifrulas.web_tasks.board;

import carobnifrulas.web_tasks.user.User;
import carobnifrulas.web_tasks.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BoardMemberService {

    private final BoardMemberRepository members;
    private final UserRepository users;

    public BoardMemberService(BoardMemberRepository members, UserRepository users) {
        this.members = members;
        this.users = users;
    }

    private boolean isGlobalAdmin(Long userId) {
        return users.findById(userId)
                .map(u -> "admin@local".equalsIgnoreCase(u.getEmail()))
                .orElse(false);
    }

    public BoardRole getRole(Long boardId, Long userId) {
        if (isGlobalAdmin(userId)) {
            return BoardRole.ADMIN; // ili OWNER ako želiš da ima full UI kontrole
        }

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

        User u = users.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalStateException("Korisnik ne postoji: " + email));

        requireUserActive(u);

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

    public List<BoardMemberRepository.AssigneeRow> listActiveAssignees(Long boardId) {
        return members.findActiveAssignees(boardId);
    }

    public void requireMember(Long boardId, Long userId) {
        members.findByIdBoardIdAndIdUserId(boardId, userId)
                .orElseThrow(() -> new IllegalStateException("Korisnik nije član ovog boarda."));
    }

    public void requireActiveMember(Long boardId, Long userId) {
        requireMember(boardId, userId);

        User u = users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Korisnik ne postoji."));

        requireUserActive(u);
    }

    public List<User> listUsersNotInBoard(Long boardId) {
        Set<Long> memberIds = members.findAllByIdBoardId(boardId).stream()
                .map(bm -> bm.getId().getUserId())
                .collect(Collectors.toSet());

        return users.findAll().stream()
                // isključi system admin
                .filter(u -> !"admin@local".equalsIgnoreCase(u.getEmail()))
                // isključi deaktivirane korisnike iz novog dodavanja na board
                .filter(User::isActive)
                // isključi već dodane članove
                .filter(u -> !memberIds.contains(u.getId()))
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public void addMemberByUserId(Long boardId, Long actorUserId, Long targetUserId, BoardRole role) {
        requireManageMembers(boardId, actorUserId);

        if (role == BoardRole.OWNER) {
            throw new IllegalStateException("Ne možeš dodati OWNER preko UI.");
        }

        User u = users.findById(targetUserId)
                .orElseThrow(() -> new IllegalStateException("Korisnik ne postoji."));

        requireUserActive(u);

        if (members.existsByIdBoardIdAndIdUserId(boardId, u.getId())) {
            throw new IllegalStateException("Korisnik je već član boarda.");
        }

        BoardMember bm = new BoardMember();
        bm.setId(new BoardMemberId(boardId, u.getId()));
        bm.setRole(role.name());
        members.save(bm);
    }

    private static void requireUserActive(User u) {
        if (!u.isActive()) {
            throw new IllegalStateException("Korisnik je deaktiviran i ne može se dodati/dodijeliti.");
        }
    }
}
