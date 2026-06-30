package carobnifrulas.web_tasks.security.model;

import carobnifrulas.web_tasks.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Locale;
import java.util.Optional;

public final class SecurityUtils {
    private static final String DEFAULT_GLOBAL_ADMIN_EMAIL = "admin@local";
    private static volatile String globalAdminEmail = DEFAULT_GLOBAL_ADMIN_EMAIL;

    private SecurityUtils() {}

    public static Optional<String> getUsername(SecurityContextImpl ctx) {
        if (ctx == null) return Optional.empty();
        Authentication auth = ctx.getAuthentication();
        if (auth == null) return Optional.empty();

        Object principal = auth.getPrincipal();
        if (principal instanceof String s) return Optional.of(s);

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return Optional.ofNullable(ud.getUsername());
        }
        return Optional.empty();
    }

    /**
     * Poziva se iz Spring konfiguracije na startu aplikacije.
     * Ako property nije popunjen, zadržavamo MVP default da stara baza nastavi raditi.
     */
    public static void setGlobalAdminEmail(String email) {
        globalAdminEmail = normalizeEmail(email).orElse(DEFAULT_GLOBAL_ADMIN_EMAIL);
    }

    public static String getGlobalAdminEmail() {
        return globalAdminEmail;
    }

    public static boolean isGlobalAdmin(User u) {
        return u != null && isGlobalAdminEmail(u.getEmail());
    }

    public static boolean isGlobalAdminEmail(String email) {
        return normalizeEmail(email)
                .map(normalized -> normalized.equals(globalAdminEmail))
                .orElse(false);
    }

    private static Optional<String> normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(email.trim().toLowerCase(Locale.ROOT));
    }
}
