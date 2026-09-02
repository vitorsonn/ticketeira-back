package com.example.back.services;

import com.example.back.dto.TicketResponseDTO;
import com.example.back.exceptions.BusinessRuleException;
import com.example.back.exceptions.ResourceNotFoundException;
import com.example.back.model.Sector;
import com.example.back.model.Ticket;
import com.example.back.model.TicketStatus;
import com.example.back.model.User;
import com.example.back.repository.SectorRepository;
import com.example.back.repository.TicketRepository;
import com.example.back.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "secretKey", "sk_test_1234567890");
        paymentService.init();
        assertEquals("sk_test_1234567890", Stripe.apiKey);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve criar PaymentIntent no Stripe com sucesso e retornar o clientSecret")
    void createPaymentIntent_Success() throws StripeException {
        // Arrange
        BigDecimal price = new BigDecimal("150.00");
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getClientSecret()).thenReturn("pi_12345_secret_67890");

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class))).thenReturn(mockIntent);

            // Act
            String clientSecret = paymentService.createPaymentIntent(price);

            // Assert
            assertNotNull(clientSecret);
            assertEquals("pi_12345_secret_67890", clientSecret);
            mockedStatic.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)), times(1));
        }
    }

    @Test
    @DisplayName("Deve retornar DTO do ticket existente se pagamento já foi processado (Idempotência)")
    void confirmPayment_TicketAlreadyExists() throws Exception {
        // Arrange
        String paymentIntentId = "pi_already_processed";
        Long sectorId = 1L;

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("succeeded");

        Ticket existingTicket = new Ticket();
        existingTicket.setPaymentIntentId(paymentIntentId);

        TicketResponseDTO expectedDTO = new TicketResponseDTO(
                "hash123", "base64qr", "Show", LocalDateTime.now(), "Arena", "Pista", new BigDecimal("100.00")
        );

        when(ticketRepository.findByPaymentIntentId(paymentIntentId)).thenReturn(Optional.of(existingTicket));
        when(ticketService.toDTO(existingTicket)).thenReturn(expectedDTO);

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve(paymentIntentId)).thenReturn(mockIntent);

            // Act
            TicketResponseDTO result = paymentService.confirmPayment(paymentIntentId, sectorId);

            // Assert
            assertNotNull(result);
            assertEquals(expectedDTO, result);
            verify(ticketRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Deve lançar exceção quando o status do pagamento não for succeeded")
    void confirmPayment_PaymentNotSucceeded_ThrowsException() {
        // Arrange
        String paymentIntentId = "pi_pending";
        Long sectorId = 1L;

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("requires_payment_method");

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve(paymentIntentId)).thenReturn(mockIntent);

            // Act & Assert
            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
                paymentService.confirmPayment(paymentIntentId, sectorId);
            });

            assertEquals("Pagamento ainda não aprovado pelo Stripe.", ex.getMessage());
        }
    }

    @Test
    @DisplayName("Deve lançar exceção quando setor não for encontrado")
    void confirmPayment_SectorNotFound_ThrowsException() {
        // Arrange
        String paymentIntentId = "pi_123";
        Long sectorId = 999L;

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("succeeded");

        when(ticketRepository.findByPaymentIntentId(paymentIntentId)).thenReturn(Optional.empty());
        when(sectorRepository.findById(sectorId)).thenReturn(Optional.empty());

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve(paymentIntentId)).thenReturn(mockIntent);

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
                paymentService.confirmPayment(paymentIntentId, sectorId);
            });

            assertEquals("Setor não encontrado com o ID: 999", ex.getMessage());
        }
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não estiver autenticado no SecurityContext")
    void confirmPayment_UserNotAuthenticated_ThrowsException() {
        // Arrange
        String paymentIntentId = "pi_123";
        Long sectorId = 1L;

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("succeeded");

        Sector sector = new Sector();
        sector.setId(sectorId);
        sector.setName("VIP");

        when(ticketRepository.findByPaymentIntentId(paymentIntentId)).thenReturn(Optional.empty());
        when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));

        // SecurityContext sem autenticação
        SecurityContextHolder.clearContext();

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve(paymentIntentId)).thenReturn(mockIntent);

            // Act & Assert
            BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
                paymentService.confirmPayment(paymentIntentId, sectorId);
            });

            assertEquals("Usuário não autenticado no sistema.", ex.getMessage());
        }
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário autenticado não for encontrado no banco")
    void confirmPayment_UserNotFoundInDatabase_ThrowsException() {
        // Arrange
        String paymentIntentId = "pi_123";
        Long sectorId = 1L;
        String userEmail = "fantasma@teste.com";

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("succeeded");

        Sector sector = new Sector();
        sector.setId(sectorId);
        sector.setName("VIP");

        when(ticketRepository.findByPaymentIntentId(paymentIntentId)).thenReturn(Optional.empty());
        when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));

        // Mock SecurityContext com usuário
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userEmail);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve(paymentIntentId)).thenReturn(mockIntent);

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
                paymentService.confirmPayment(paymentIntentId, sectorId);
            });

            assertEquals("Usuário com email fantasma@teste.com não existe no banco!", ex.getMessage());
        }
    }

    @Test
    @DisplayName("Deve confirmar pagamento com sucesso, gerar QR code e salvar novo ticket")
    void confirmPayment_Success() throws Exception {
        // Arrange
        String paymentIntentId = "pi_success_123";
        Long sectorId = 1L;
        String userEmail = "comprador@teste.com";

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("succeeded");
        when(mockIntent.getAmount()).thenReturn(15000L); // R$ 150.00 em centavos

        Sector sector = new Sector();
        sector.setId(sectorId);
        sector.setName("Pista Premium");

        User user = new User();
        user.setEmail(userEmail);

        when(ticketRepository.findByPaymentIntentId(paymentIntentId)).thenReturn(Optional.empty());
        when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));

        // Mock SecurityContext
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userEmail);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponseDTO expectedDTO = new TicketResponseDTO(
                "hash", "mockBase64QRCodeImage", "Show", LocalDateTime.now(), "Local", "Pista Premium", new BigDecimal("150.00")
        );
        when(ticketService.toDTO(any(Ticket.class))).thenReturn(expectedDTO);

        try (MockedStatic<PaymentIntent> mockedStatic = mockStatic(PaymentIntent.class)) {
            mockedStatic.when(() -> PaymentIntent.retrieve(paymentIntentId)).thenReturn(mockIntent);

            // Act
            TicketResponseDTO result = paymentService.confirmPayment(paymentIntentId, sectorId);

            // Assert
            assertNotNull(result);
            assertEquals(expectedDTO, result);
            verify(ticketRepository, times(1)).save(any(Ticket.class));
            verify(ticketService, times(1)).toDTO(any(Ticket.class));
        }
    }
}
