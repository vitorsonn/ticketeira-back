package com.example.back.controller;

import com.example.back.config.ControllerAdvice;
import com.example.back.dto.AuthenticationDTO;
import com.example.back.dto.UserRequestDTO;
import com.example.back.exceptions.BusinessRuleException;
import com.example.back.model.User;
import com.example.back.model.UserRole;
import com.example.back.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(ControllerAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Nested
    @DisplayName("POST /users/register")
    class RegisterTests {

        @Test
        @DisplayName("Deve registrar um usuário com sucesso e retornar HTTP 200")
        void register_Success() throws Exception {
            UserRequestDTO requestDTO = new UserRequestDTO("novo@teste.com", "senha123");
            User savedUser = new User();
            savedUser.setEmail(requestDTO.email());
            savedUser.setRole(UserRole.USER);

            when(userService.register(any(UserRequestDTO.class))).thenReturn(savedUser);

            mockMvc.perform(post("/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Usuário cadastrado com sucesso"));

            verify(userService, times(1)).register(any(UserRequestDTO.class));
        }

        @Test
        @DisplayName("Deve retornar HTTP 400 quando a senha for menor que 6 caracteres")
        void register_PasswordTooShort_Returns400() throws Exception {
            UserRequestDTO requestDTO = new UserRequestDTO("novo@teste.com", "123");

            mockMvc.perform(post("/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Erro de validação"))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("password"));

            verify(userService, never()).register(any());
        }

        @Test
        @DisplayName("Deve retornar HTTP 400 com erro padronizado quando o e-mail já estiver cadastrado")
        void register_EmailAlreadyExists_Returns400() throws Exception {
            UserRequestDTO requestDTO = new UserRequestDTO("duplicado@teste.com", "senha123");

            when(userService.register(any(UserRequestDTO.class)))
                    .thenThrow(new BusinessRuleException("Este e-mail já está cadastrado."));

            mockMvc.perform(post("/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Regra de negócio violada"))
                    .andExpect(jsonPath("$.message").value("Este e-mail já está cadastrado."))
                    .andExpect(jsonPath("$.path").value("/users/register"));

            verify(userService, times(1)).register(any(UserRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("POST /users/login")
    class LoginTests {

        @Test
        @DisplayName("Deve autenticar com sucesso e retornar token JWT e dados do usuário")
        void login_Success() throws Exception {
            AuthenticationDTO authDTO = new AuthenticationDTO("user@teste.com", "senha123");

            User mockUser = new User();
            mockUser.setEmail("user@teste.com");
            mockUser.setRole(UserRole.USER);

            Authentication mockAuth = mock(Authentication.class);
            when(mockAuth.getPrincipal()).thenReturn(mockUser);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mockAuth);
            when(userService.gerarToken(mockUser)).thenReturn("fake-jwt-token-12345");

            mockMvc.perform(post("/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("fake-jwt-token-12345"))
                    .andExpect(jsonPath("$.role").value("USER"))
                    .andExpect(jsonPath("$.email").value("user@teste.com"));

            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(userService, times(1)).gerarToken(mockUser);
        }

        @Test
        @DisplayName("Deve retornar HTTP 401 quando as credenciais forem inválidas")
        void login_BadCredentials_Returns401() throws Exception {
            AuthenticationDTO authDTO = new AuthenticationDTO("user@teste.com", "senha_errada");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authDTO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Não autorizado"))
                    .andExpect(jsonPath("$.message").value("Email ou senha inválidos."))
                    .andExpect(jsonPath("$.path").value("/users/login"));

            verify(userService, never()).gerarToken(any());
        }
    }
}
