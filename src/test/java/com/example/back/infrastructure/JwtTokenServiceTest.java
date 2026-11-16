package com.example.back.infrastructure;

import com.example.back.model.User;
import com.example.back.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp(){
        jwtTokenService = new JwtTokenService("segredoSuperSecretoComTamanhoMinimoDe32BytesParaHS256!");
    }


    @Test
    @DisplayName("Deve gerar e validar token JWT com sucesso")
    void shouldGenerateAndValidateToken() {
        User user = new User();
        user.setEmail("auth@teste.com");
        user.setRole(UserRole.USER);

        String token = jwtTokenService.generateToken(user);
        String subject = jwtTokenService.validateToken(token);

        assertNotNull(token);
        assertEquals("auth@teste.com", subject);
    }

    @Test
    @DisplayName("Deve retornar null quando o token for inválido")
    void shouldReturnNullWhenTokenIsInvalid() {
        String subject = jwtTokenService.validateToken("token-invalido");
        assertNull(subject);
    }
}

