package com.example.back.controller;
import com.example.back.application.port.PaymentGateway;
import com.example.back.application.port.UserContext;
import com.example.back.application.usecase.ConfirmPaymentUseCase;
import com.example.back.dto.PaymentRequestDTO;
import com.example.back.dto.TicketResponseDTO;
import com.example.back.model.Ticket;
import com.example.back.model.User;
import com.example.back.services.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    private final PaymentGateway paymentGateway;
    private final ConfirmPaymentUseCase confirmPaymentUseCase;
    private final TicketService ticketService;
    private final UserContext userContext;

    public PaymentController(PaymentGateway paymentGateway,
                             ConfirmPaymentUseCase confirmPaymentUseCase,
                             TicketService ticketService,
                             UserContext userContext) {
        this.paymentGateway = paymentGateway;
        this.confirmPaymentUseCase = confirmPaymentUseCase;
        this.ticketService = ticketService;
        this.userContext = userContext;
    }

    @PostMapping("/intent")
    public String createIntent(@RequestBody PaymentRequestDTO paymentRequest) {
        return paymentGateway.createPaymentIntent(paymentRequest.amount());
    }

    @GetMapping("intent/confirm/{id}")
    public ResponseEntity<TicketResponseDTO> confirm(@PathVariable String id, @RequestParam Long sectorId) {
        String userEmail = userContext.getRequiredCurrentUser().getEmail();
        Ticket ticket = confirmPaymentUseCase.execute(id, sectorId, userEmail);
        TicketResponseDTO response = ticketService.toDTO(ticket);
        return ResponseEntity.ok(response);
    }


}
