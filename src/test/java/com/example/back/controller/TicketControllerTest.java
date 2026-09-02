package com.example.back.controller;

import com.example.back.config.ControllerAdvice;
import com.example.back.config.SecurityFilter;
import com.example.back.dto.TicketRequestDTO;
import com.example.back.dto.TicketResponseDTO;
import com.example.back.exceptions.BusinessRuleException;
import com.example.back.exceptions.ResourceNotFoundException;
import com.example.back.model.Ticket;
import com.example.back.model.TicketStatus;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TicketController.class)
@Import(ControllerAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TicketService ticketService;

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
        user.setId(UUID.randomUUID());
        user.setEmail("user@teste.com");
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return user;
    }

    @Nested
    @DisplayName("POST /tickets")
    class BuyTicketTests {

        @Test
        @DisplayName("Deve comprar ingresso com sucesso e retornar HTTP 201")
        void buyTicket_Success() throws Exception {
            User user = mockAuthenticatedUser();
            TicketRequestDTO requestDTO = new TicketRequestDTO(1L);

            Ticket ticket = new Ticket();
            ticket.setId(10L);
            ticket.setQrCode("qr-uuid-123");
            ticket.setStatus(TicketStatus.ACTIVE);

            TicketResponseDTO responseDTO = new TicketResponseDTO(
                    "qr-uuid-123", "base64qr", "Show de Rock", LocalDateTime.now().plusDays(10), "Estádio", "Pista", new BigDecimal("100.00")
            );

            when(ticketService.buyTicket(any(TicketRequestDTO.class), eq(user))).thenReturn(ticket);
            when(ticketService.toDTO(ticket)).thenReturn(responseDTO);

            mockMvc.perform(post("/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ticketHash").value("qr-uuid-123"))
                    .andExpect(jsonPath("$.eventName").value("Show de Rock"))
                    .andExpect(jsonPath("$.sector").value("Pista"));

            verify(ticketService, times(1)).buyTicket(any(TicketRequestDTO.class), eq(user));
            verify(ticketService, times(1)).toDTO(ticket);
        }

        @Test
        @DisplayName("Deve retornar HTTP 401 quando usuário não estiver autenticado")
        void buyTicket_Unauthenticated_Returns401() throws Exception {
            SecurityContextHolder.clearContext();
            TicketRequestDTO requestDTO = new TicketRequestDTO(1L);

            mockMvc.perform(post("/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isUnauthorized());

            verify(ticketService, never()).buyTicket(any(), any());
        }

        @Test
        @DisplayName("Deve retornar HTTP 400 quando o setor estiver esgotado")
        void buyTicket_SectorSoldOut_Returns400() throws Exception {
            User user = mockAuthenticatedUser();
            TicketRequestDTO requestDTO = new TicketRequestDTO(1L);

            when(ticketService.buyTicket(any(TicketRequestDTO.class), eq(user)))
                    .thenThrow(new BusinessRuleException("Setor esgotado! Não há mais ingressos disponiveis"));

            mockMvc.perform(post("/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Regra de negócio violada"))
                    .andExpect(jsonPath("$.message").value("Setor esgotado! Não há mais ingressos disponiveis"));

            verify(ticketService, times(1)).buyTicket(any(TicketRequestDTO.class), eq(user));
        }

        @Test
        @DisplayName("Deve retornar HTTP 404 quando o setor não for encontrado")
        void buyTicket_SectorNotFound_Returns404() throws Exception {
            User user = mockAuthenticatedUser();
            TicketRequestDTO requestDTO = new TicketRequestDTO(999L);

            when(ticketService.buyTicket(any(TicketRequestDTO.class), eq(user)))
                    .thenThrow(new ResourceNotFoundException("Setor não encontrado"));

            mockMvc.perform(post("/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Recurso não encontrado"))
                    .andExpect(jsonPath("$.message").value("Setor não encontrado"));

            verify(ticketService, times(1)).buyTicket(any(TicketRequestDTO.class), eq(user));
        }
    }

    @Nested
    @DisplayName("GET /tickets/my-tickets")
    class MyTicketsTests {

        @Test
        @DisplayName("Deve retornar a lista de ingressos do usuário autenticado")
        void getMyTickets_Success() throws Exception {
            User user = mockAuthenticatedUser();

            Ticket ticket1 = new Ticket();
            ticket1.setId(1L);

            TicketResponseDTO responseDTO = new TicketResponseDTO(
                    "qr-1", "b64", "Show", LocalDateTime.now(), "Arena", "VIP", new BigDecimal("150.00")
            );

            when(ticketService.listByUser(user)).thenReturn(List.of(ticket1));
            when(ticketService.toDTO(ticket1)).thenReturn(responseDTO);

            mockMvc.perform(get("/tickets/my-tickets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].ticketHash").value("qr-1"))
                    .andExpect(jsonPath("$[0].eventName").value("Show"));

            verify(ticketService, times(1)).listByUser(user);
        }
    }
}
