package com.shopzone.auth_service;

import com.shopzone.auth_service.dto.AuthResponse;
import com.shopzone.auth_service.dto.LoginRequest;
import com.shopzone.auth_service.dto.RegisterRequest;
import com.shopzone.auth_service.exception.UserAlreadyExistsException;
import com.shopzone.auth_service.exception.UserNotFoundException;
import com.shopzone.auth_service.model.Role;
import com.shopzone.auth_service.model.User;
import com.shopzone.auth_service.repository.UserRepository;
import com.shopzone.auth_service.security.JwtService;
import com.shopzone.auth_service.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@gmail.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setName("Test User");
        registerRequest.setEmail("test@gmail.com");
        registerRequest.setPassword("123456");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@gmail.com");
        loginRequest.setPassword("123456");
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenReturn(testUser);
        when(jwtService.generateToken(any())).thenReturn("mockToken");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("test@gmail.com", response.getEmail());
        assertEquals("mockToken", response.getToken());
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(any())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginSuccessfully() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("mockToken");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mockToken", response.getToken());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    void shouldThrowExceptionWhenPasswordWrong() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });
    }
}