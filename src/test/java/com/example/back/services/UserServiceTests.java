package com.example.back.services;

import com.example.back.dto.UserRequestDTO;
import com.example.back.model.User;
import com.example.back.model.UserRole;
import com.example.back.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        // Injeta a secret key necessária para geração e validação de tokens JWT
        ReflectionTestUtils.setField(userService, "secret", "my-super-secret-key-that-is-at-least-256-bits-long!");
    }

    @Test
    @DisplayName("Deve registrar um usuário comum com senha criptografada com sucesso")
    void register_CommonUser_Success() {

        UserRequestDTO userRequestDTO = new UserRequestDTO("cliente@teste.com", "senha123");
        when(userRepository.findByEmail(userRequestDTO.email())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.register(userRequestDTO);

        assertNotNull(savedUser);
        assertEquals("cliente@teste.com", savedUser.getEmail());
        assertEquals(UserRole.USER, savedUser.getRole());
        assertNotEquals("senha123", savedUser.getPassword()); // Valida hash BCrypt
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Deve cadastrar como ADMIN se o e-mail for admin@ticketeira.com")
    void register_AdminUser_Success() {
        // Arrange
        UserRequestDTO dto = new UserRequestDTO("admin@ticketeira.com", "admin123");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User savedUser = userService.register(dto);

        // Assert
        assertEquals(UserRole.ADMIN, savedUser.getRole());
    }

    @Test
    @DisplayName("Deve lançar exceção se e-mail já existir")
    void register_DuplicateEmail_ThrowsException() {
        // Arrange
        UserRequestDTO dto = new UserRequestDTO("duplicado@teste.com", "senha123");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(new User()));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.register(dto));
        assertEquals("Este e-mail já está cadastrado.", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve gerar e validar token JWT corretamente")
    void tokenGenerationAndValidation() {
        // Arrange
        User user = new User();
        user.setEmail("auth@teste.com");
        user.setRole(UserRole.USER);

        // Act
        String token = userService.gerarToken(user);
        String subject = userService.validateToken(token);

        // Assert
        assertNotNull(token);
        assertEquals("auth@teste.com", subject);
    }










}
