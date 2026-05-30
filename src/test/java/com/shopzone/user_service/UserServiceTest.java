package com.shopzone.user_service;

import com.shopzone.user_service.dto.UpdateUserRequest;
import com.shopzone.user_service.dto.UserResponse;
import com.shopzone.user_service.exception.UserNotFoundException;
import com.shopzone.user_service.model.Role;
import com.shopzone.user_service.model.User;
import com.shopzone.user_service.repository.UserRepository;
import com.shopzone.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@gmail.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();
    }

    @Test
    void shouldGetUserByIdSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals("test@gmail.com", response.getEmail());
        assertEquals("Test User", response.getName());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundById() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserById(999L);
        });
    }

    @Test
    void shouldGetAllUsersSuccessfully() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserResponse> responses = userService.getAllUsers();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("test@gmail.com", responses.get(0).getEmail());
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Updated Name");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        UserResponse response = userService.updateUser(1L, request);

        assertNotNull(response);
        verify(userRepository, times(1)).save(any());
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.deleteUser(1L);

        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentUser() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            userService.deleteUser(999L);
        });
    }
}