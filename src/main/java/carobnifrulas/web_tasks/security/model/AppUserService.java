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
        return users.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
    }

    public List<User> findAllUsers() {
        return users.findAllByOrderByIdDesc();
    }

    public boolean existsByEmail(String email) {
        return users.existsByEmailIgnoreCase(email);
    }

    /** Kreira user-a i vrati mu privremenu lozinku (plain) da admin može copy/paste. */
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

        users.save(u);

        return new CreatedUserResult(u, tempPassword);
    }

    /** Admin reset lozinke: postavi novu temp i force change. */
    @Transactional
    public String resetPassword(Long userId) {
        User u = users.findById(userId).orElseThrow();
        String tempPassword = generateTempPassword(10);
        u.setPasswordHash(encoder.encode(tempPassword));
        u.setMustChangePassword(true);
        users.save(u);
        return tempPassword;
    }

    /** User promjena lozinke (npr. ChangePasswordView). */
    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User u = users.findById(userId).orElseThrow();
        u.setPasswordHash(encoder.encode(newPassword));
        u.setMustChangePassword(false);
        users.save(u);
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