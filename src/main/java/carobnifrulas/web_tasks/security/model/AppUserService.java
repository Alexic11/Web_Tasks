package carobnifrulas.web_tasks.security.model;

import carobnifrulas.web_tasks.user.User;
import carobnifrulas.web_tasks.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class AppUserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AppUserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public User requireByEmail(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
    }

    public List<User> findAllUsers() {
        return users.findAllByOrderByIdDesc();
    }

    public List<User> findActiveUsers() {
        return users.findAllByActiveTrueOrderByFullNameAsc();
    }

    public boolean existsByEmail(String email) {
        return users.existsByEmailIgnoreCase(email);
    }

    @Transactional
    public CreatedUserResult createUserWithTempPassword(String email, String fullName) {
        if (existsByEmail(email)) {
            throw new IllegalStateException("Email već postoji.");
        }

        String tempPassword = generateTempPassword(10);

        User u = new User();
        u.setEmail(email.trim().toLowerCase());
        u.setFullName(fullName.trim());
        u.setPasswordHash(encoder.encode(tempPassword));
        u.setMustChangePassword(true);
        u.setActive(true);

        users.save(u);

        return new CreatedUserResult(u, tempPassword);
    }

    @Transactional
    public String resetPassword(Long userId) {
        User u = users.findById(userId).orElseThrow(() -> new IllegalStateException("User not found."));
        String tempPassword = generateTempPassword(10);
        u.setPasswordHash(encoder.encode(tempPassword));
        u.setMustChangePassword(true);
        users.save(u);
        return tempPassword;
    }

    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User u = users.findById(userId).orElseThrow(() -> new IllegalStateException("User not found."));

        if (!u.isActive()) {
            throw new IllegalStateException("Korisnički nalog je deaktiviran.");
        }

        u.setPasswordHash(encoder.encode(newPassword));
        u.setMustChangePassword(false);
        users.save(u);
    }

    /**
     * Stari hard delete više ne koristimo jer user može imati FK veze
     * (board_members, taskovi, activity, komentari, notifikacije...).
     * Zadržavamo metodu radi kompatibilnosti sa starim pozivima, ali ona sada radi soft deactivate.
     */
    @Transactional
    public void deleteUser(Long userId) {
        deactivateUser(userId, null);
    }

    @Transactional
    public void deactivateUser(Long userId, Long actorUserId) {
        User u = users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found."));

        if (isSystemAdmin(u)) {
            throw new IllegalStateException("Ne možeš deaktivirati admin nalog.");
        }

        if (actorUserId != null && actorUserId.equals(userId)) {
            throw new IllegalStateException("Ne možeš deaktivirati svoj nalog dok si ulogovan.");
        }

        if (!u.isActive()) {
            return;
        }

        u.setActive(false);
        users.save(u);
    }

    @Transactional
    public void activateUser(Long userId) {
        User u = users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found."));

        if (u.isActive()) {
            return;
        }

        u.setActive(true);
        users.save(u);
    }

    public void requireActiveUser(Long userId) {
        User u = users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found."));

        if (!u.isActive()) {
            throw new IllegalStateException("Korisnički nalog je deaktiviran.");
        }
    }

    public boolean isActiveUser(Long userId) {
        return users.findById(userId)
                .map(User::isActive)
                .orElse(false);
    }

    private static boolean isSystemAdmin(User u) {
        return SecurityUtils.isGlobalAdmin(u);
    }

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#";
    private static final SecureRandom RND = new SecureRandom();

    private String generateTempPassword(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(ALPHABET.charAt(RND.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public record CreatedUserResult(User user, String tempPassword) {}

    public java.util.Optional<User> findById(Long userId) {
        return users.findById(userId);
    }
}
