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
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {


    private final UserRepository userRepository;


    private final SectorRepository sectorRepository;


    private final TicketRepository ticketRepository;


    private final QRCodeService qrCodeService;


    private final TicketService ticketService;

    @Value("${stripe.api.key}")
    private String secretKey;

    public PaymentService(UserRepository userRepository, SectorRepository sectorRepository, TicketRepository ticketRepository, QRCodeService qrCodeService, TicketService ticketService) {
        this.userRepository = userRepository;
        this.sectorRepository = sectorRepository;
        this.ticketRepository = ticketRepository;
        this.qrCodeService = qrCodeService;
        this.ticketService = ticketService;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    public String createPaymentIntent(BigDecimal price) throws StripeException {
        long amountInCents = price.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("brl")
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        return intent.getClientSecret();
    }

    public TicketResponseDTO confirmPayment(String paymentIntentId, Long sectorId) throws Exception {
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);

        if (!"succeeded".equals(intent.getStatus())) {
            throw new BusinessRuleException("Pagamento ainda não aprovado pelo Stripe.");
        }

        var ticketExistente = ticketRepository.findByPaymentIntentId(paymentIntentId);
        if (ticketExistente.isPresent()) {
            return ticketService.toDTO(ticketExistente.get());
        }

        String hashUnico = UUID.randomUUID().toString();

        Sector setor = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado com o ID: " + sectorId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new BusinessRuleException("Usuário não autenticado no sistema.");
        }

        String emailUser = auth.getName();
        User user = userRepository.findByEmail(emailUser)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com email " + emailUser + " não existe no banco!"));

        Ticket novoTicket = new Ticket();
        novoTicket.setPaymentIntentId(paymentIntentId);
        novoTicket.setPurchaseDate(LocalDateTime.now());
        novoTicket.setPrice(new BigDecimal(intent.getAmount()).divide(new BigDecimal(100)));
        novoTicket.setUser(user);
        novoTicket.setQrCode(hashUnico);
        novoTicket.setSector(setor);
        novoTicket.setStatus(TicketStatus.ACTIVE);

        ticketRepository.save(novoTicket);
        return ticketService.toDTO(novoTicket);
    }
}
