package com.example.back.application.usecase;

import com.example.back.application.port.PaymentGateway;
import com.example.back.dto.PaymentInfo;
import com.example.back.exceptions.BusinessRuleException;
import com.example.back.exceptions.ResourceNotFoundException;
import com.example.back.model.Sector;
import com.example.back.model.Ticket;
import com.example.back.model.TicketStatus;
import com.example.back.model.User;
import com.example.back.repository.SectorRepository;
import com.example.back.repository.TicketRepository;
import com.example.back.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmPaymentUseCaseTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ConfirmPaymentUseCase confirmPaymentUseCase;

    @Test
    @DisplayName("Deve confirmar pagamento com sucesso e criar novo ticket")
    void execute_Success() {
        String paymentIntentId = "pi_success_123";
        Long sectorId = 1L;
        String userEmail = "comprador@teste.com";

        PaymentInfo paymentInfo = new PaymentInfo(paymentIntentId, "succeeded", new BigDecimal("150.00"));
        when(paymentGateway.getPayment(paymentIntentId)).thenReturn(paymentInfo);
        when(ticketRepository.findByPaymentIntentId(paymentIntentId)).thenReturn(Optional.empty());

        Sector sector = new Sector();
        sector.setId(sectorId);
        sector.setName("Pista Premium");
        sector.setCapacity(100);
        when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));

        User user = new User();
        user.setEmail(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket result = confirmPaymentUseCase.execute(paymentIntentId, sectorId, userEmail);

        assertNotNull(result);
        assertEquals(TicketStatus.ACTIVE, result.getStatus());
        assertEquals(sector, result.getSector());
        assertEquals(user, result.getUser());
        assertEquals(new BigDecimal("150.00"), result.getPrice());
        assertNotNull(result.getQrCode());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Deve retornar ticket existente se pagamento já foi processado (Idempotência)")
    void execute_TicketAlreadyExists() {
        String paymentIntentId = "pi_already_processed";
        Long sectorId = 1L;
        String userEmail = "user@teste.com";

        PaymentInfo paymentInfo = new PaymentInfo(paymentIntentId, "succeeded", new BigDecimal("100.00"));
        when(paymentGateway.getPayment(paymentIntentId)).thenReturn(paymentInfo);

        Ticket existingTicket = new Ticket();
        existingTicket.setPaymentIntentId(paymentIntentId);
        when(ticketRepository.findByPaymentIntentId(paymentIntentId)).thenReturn(Optional.of(existingTicket));

        Ticket result = confirmPaymentUseCase.execute(paymentIntentId, sectorId, userEmail);

        assertNotNull(result);
        assertEquals(existingTicket, result);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando pagamento não foi aprovado")
    void execute_PaymentNotSucceeded_ThrowsException() {
        String paymentIntentId = "pi_pending";

        PaymentInfo paymentInfo = new PaymentInfo(paymentIntentId, "requires_payment_method", new BigDecimal("100.00"));
        when(paymentGateway.getPayment(paymentIntentId)).thenReturn(paymentInfo);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
            confirmPaymentUseCase.execute(paymentIntentId, 1L, "user@teste.com");
        });

        assertEquals("Pagamento ainda não aprovado", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção quando setor não for encontrado")
    void execute_SectorNotFound_ThrowsException() {
        String paymentIntentId = "pi_123";
        Long sectorId = 999L;

        PaymentInfo paymentInfo = new PaymentInfo(paymentIntentId, "succeeded", new BigDecimal("100.00"));
        when(paymentGateway.getPayment(paymentIntentId)).thenReturn(paymentInfo);
        when(ticketRepository.findByPaymentIntentId(paymentIntentId)).thenReturn(Optional.empty());
        when(sectorRepository.findById(sectorId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
            confirmPaymentUseCase.execute(paymentIntentId, sectorId, "user@teste.com");
        });

        assertEquals("Setor não encontrado", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não for encontrado")
    void execute_UserNotFound_ThrowsException() {
        String paymentIntentId = "pi_123";
        Long sectorId = 1L;
        String userEmail = "fantasma@teste.com";

        PaymentInfo paymentInfo = new PaymentInfo(paymentIntentId, "succeeded", new BigDecimal("100.00"));
        when(paymentGateway.getPayment(paymentIntentId)).thenReturn(paymentInfo);
        when(ticketRepository.findByPaymentIntentId(paymentIntentId)).thenReturn(Optional.empty());

        Sector sector = new Sector();
        sector.setId(sectorId);
        when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
            confirmPaymentUseCase.execute(paymentIntentId, sectorId, userEmail);
        });

        assertEquals("Usuário não encontrado", ex.getMessage());
    }
}
