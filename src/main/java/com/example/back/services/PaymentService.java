package com.example.back.services;
import com.example.back.dto.TicketResponseDTO;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private TicketService ticketService;

    @Value("${stripe.api.key}")
    private String secretKey;

    @PostConstruct
    public void init(){
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


        PaymentIntent intent;
        try {
            intent = PaymentIntent.retrieve(paymentIntentId);
        } catch (Exception e) {

            throw e;
        }

        if (!"succeeded".equals(intent.getStatus())) {
            throw new RuntimeException("Pagamento ainda não aprovado pelo Stripe.");
        }

        System.out.println("=== [DEBUG 3] Verificando se ticket já existe no banco...");
        var ticketExistente = ticketRepository.findByPaymentIntentId(paymentIntentId);
        if (ticketExistente.isPresent()) {
            return ticketService.toDTO(ticketExistente.get());
        }

        String qrCodeImageBase64 = null;
        String hashUnico = UUID.randomUUID().toString();

        try {

            System.out.println("=== Buscando Setor no banco com ID: " + sectorId);
            Sector setor = sectorRepository.findById(sectorId)
                    .orElseThrow(() -> new RuntimeException("nenhum setor encontrado no banco!"));
            System.out.println("Setor encontrado: " + setor.getName());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null) {
                System.err.println("!!! ERRO: Objeto Authentication está NULO. O Spring Security não identificou o usuário.");
                throw new RuntimeException("Usuário não autenticado no sistema.");
            }

            String emailUser = auth.getName();

            User user = userRepository.findByEmail(emailUser)
                    .orElseThrow(() -> new RuntimeException("Usuário com email " + emailUser + " não existe no banco!"));

            Ticket novoTicket = new Ticket();
            novoTicket.setPaymentIntentId(paymentIntentId);
            novoTicket.setPurchaseDate(LocalDateTime.now());
            novoTicket.setPrice(new BigDecimal(intent.getAmount()).divide(new BigDecimal(100)));
            novoTicket.setUser(user);
            novoTicket.setQrCode(hashUnico);
            novoTicket.setSector(setor);
            novoTicket.setStatus(TicketStatus.ACTIVE);


            qrCodeImageBase64 = qrCodeService.generateQRcodeBase64(hashUnico);
            System.out.println("===QR Code gerado com sucesso (Base64 size: " + qrCodeImageBase64.length() + ")");
            ticketRepository.save(novoTicket);
            return ticketService.toDTO(novoTicket);

        } catch (Exception e) {
            System.err.println("\n************************************************");
            System.err.println("!!! ERRO CRÍTICO NO PROCESSAMENTO DO TICKET !!!");
            System.err.println("Mensagem: " + e.getMessage());
            System.err.println("Causa: " + e.getCause());
            e.printStackTrace();
            System.err.println("************************************************\n");
            throw e;
        }

    }

}
