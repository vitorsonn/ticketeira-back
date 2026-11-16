package com.example.back.controller;

import com.example.back.application.port.PaymentGateway;
import com.example.back.application.usecase.ConfirmPaymentUseCase;
import com.example.back.config.ControllerAdvice;
import com.example.back.config.SecurityFilter;
import com.example.back.dto.PaymentRequestDTO;
import com.example.back.dto.TicketResponseDTO;
import com.example.back.model.Ticket;
import com.example.back.model.User;
import com.example.back.services.TicketService;
import com.example.back.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@Import(ControllerAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private PaymentGateway paymentGateway;

    @MockitoBean
    private ConfirmPaymentUseCase confirmPaymentUseCase;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private com.example.back.application.port.UserContext userContext;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User mockAuthenticatedUser() {
        User user = new User();
        user.setEmail("user@teste.com");
        var auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userContext.getRequiredCurrentUser()).thenReturn(user);
        return user;
    }

    @Nested
    @DisplayName("POST /api/payments/intent")
    class CreateIntentTests {

        @Test
        @DisplayName("Deve criar intent de pagamento e retornar o clientSecret")
        void createIntent_Success() throws Exception {
            BigDecimal amount = new BigDecimal("120.00");
            PaymentRequestDTO requestDTO = new PaymentRequestDTO(amount);

            when(paymentGateway.createPaymentIntent(amount)).thenReturn("pi_secret_12345");

            mockMvc.perform(post("/api/payments/intent")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("pi_secret_12345"));

            verify(paymentGateway, times(1)).createPaymentIntent(amount);
        }
    }

    @Nested
    @DisplayName("GET /api/payments/intent/confirm/{id}")
    class ConfirmPaymentTests {

        @Test
        @DisplayName("Deve confirmar pagamento com sucesso e retornar TicketResponseDTO")
        void confirmPayment_Success() throws Exception {
            User user = mockAuthenticatedUser();
            String paymentIntentId = "pi_123";
            Long sectorId = 1L;

            Ticket ticket = new Ticket();
            ticket.setId(1L);

            TicketResponseDTO responseDTO = new TicketResponseDTO(
                    "hash-qr-123", "base64qr", "Show Musical", LocalDateTime.now(), "Teatro", "Plateia", new BigDecimal("120.00")
            );

            when(confirmPaymentUseCase.execute(eq(paymentIntentId), eq(sectorId), eq("user@teste.com"))).thenReturn(ticket);
            when(ticketService.toDTO(ticket)).thenReturn(responseDTO);

            mockMvc.perform(get("/api/payments/intent/confirm/{id}", paymentIntentId)
                            .param("sectorId", sectorId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticketHash").value("hash-qr-123"))
                    .andExpect(jsonPath("$.eventName").value("Show Musical"))
                    .andExpect(jsonPath("$.sector").value("Plateia"));

            verify(confirmPaymentUseCase, times(1)).execute(eq(paymentIntentId), eq(sectorId), eq("user@teste.com"));
            verify(ticketService, times(1)).toDTO(ticket);
        }

        @Test
        @DisplayName("Deve retornar HTTP 500 caso ocorra erro inesperado na confirmação de pagamento")
        void confirmPayment_Error_Returns500() throws Exception {
            User user = mockAuthenticatedUser();
            String paymentIntentId = "pi_error";
            Long sectorId = 1L;

            when(confirmPaymentUseCase.execute(eq(paymentIntentId), eq(sectorId), eq("user@teste.com")))
                    .thenThrow(new RuntimeException("Falha na confirmação"));

            mockMvc.perform(get("/api/payments/intent/confirm/{id}", paymentIntentId)
                            .param("sectorId", sectorId.toString()))
                    .andExpect(status().isInternalServerError());

            verify(confirmPaymentUseCase, times(1)).execute(eq(paymentIntentId), eq(sectorId), eq("user@teste.com"));
        }
    }
}
