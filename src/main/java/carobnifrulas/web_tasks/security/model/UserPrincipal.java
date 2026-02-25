package carobnifrulas.web_tasks.security.model;

import carobnifrulas.web_tasks.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final User user;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.user = user;

        // MVP: svi su ROLE_USER, a admin@local je ROLE_ADMIN
        if ("admin@local".equalsIgnoreCase(user.getEmail())) {
            this.authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_USER")
            );
        } else {
            this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }

    public User getUser() {
        return user;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return user.getPasswordHash(); }
    @Override public String getUsername() { return user.getEmail(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}