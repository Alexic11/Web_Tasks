package carobnifrulas.web_tasks.security.model;

import carobnifrulas.web_tasks.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Optional;

public final class SecurityUtils {
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

    public static boolean isGlobalAdmin(User u) {
        return u != null && "admin@local".equalsIgnoreCase(u.getEmail());
    }

}