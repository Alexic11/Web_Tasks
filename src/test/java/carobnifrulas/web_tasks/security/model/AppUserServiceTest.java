package carobnifrulas.web_tasks.security.model;

import carobnifrulas.web_tasks.user.User;
import carobnifrulas.web_tasks.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppUserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AppUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new AppUserService(userRepository, passwordEncoder);
    }

    @Test
    void createUserWithTempPasswordShouldCreateActiveUser() {
        when(userRepository.existsByEmailIgnoreCase("john@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode(any()))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createUserWithTempPassword(
                "John@Example.com",
                "John Smith"
        );

        assertNotNull(result);
        assertNotNull(result.tempPassword());
        assertEquals(10, result.tempPassword().length());

        User createdUser = result.user();

        assertEquals("john@example.com", createdUser.getEmail());
        assertEquals("John Smith", createdUser.getFullName());
        assertEquals("encoded-password", createdUser.getPasswordHash());
        assertTrue(createdUser.isMustChangePassword());
        assertTrue(createdUser.isActive());

        verify(passwordEncoder).encode(result.tempPassword());
        verify(userRepository).save(createdUser);
    }

    @Test
    void createUserWithTempPasswordShouldRejectDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("john@example.com"))
                .thenReturn(true);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.createUserWithTempPassword(
                        "john@example.com",
                        "John Smith"
                )
        );

        assertEquals("Email već postoji.", ex.getMessage());

        verify(userRepository, never()).save(any());
    }
}