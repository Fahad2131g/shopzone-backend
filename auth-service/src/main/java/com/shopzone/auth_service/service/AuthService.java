package com.shopzone.auth_service.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.shopzone.auth_service.dto.AuthResponse;
import com.shopzone.auth_service.dto.GoogleAuthRequest;
import com.shopzone.auth_service.dto.LoginRequest;
import com.shopzone.auth_service.dto.RegisterRequest;
import com.shopzone.auth_service.dto.UserResponse;
import com.shopzone.auth_service.exception.UserAlreadyExistsException;
import com.shopzone.auth_service.exception.UserNotFoundException;
import com.shopzone.auth_service.model.Role;
import com.shopzone.auth_service.model.User;
import com.shopzone.auth_service.repository.UserRepository;
import com.shopzone.auth_service.security.GoogleTokenVerifier;
import com.shopzone.auth_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Wrong password");
        }
        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse googleLogin(GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        Boolean emailVerified = payload.getEmailVerified();

        if (email == null || emailVerified == null || !emailVerified) {
            throw new RuntimeException("Google account email not verified");
        }
        if (name == null || name.isBlank()) {
            name = email;
        }

        final String finalName = name;
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .name(finalName)
                    .email(email)
                    // Random unusable password — this account can only sign in via Google
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(Role.USER)
                    .build();
            return userRepository.save(newUser);
        });

        String token = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .build())
                .collect(Collectors.toList());
    }

    public UserResponse updateUserRole(Long id, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        user.setRole(Role.valueOf(role));
        userRepository.save(user);
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
    }
}