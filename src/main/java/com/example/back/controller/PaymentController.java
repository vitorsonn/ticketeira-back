package com.example.back.controller;
import com.example.back.dto.PaymentRequestDTO;
import com.example.back.dto.TicketResponseDTO;
import com.example.back.model.Ticket;
import com.example.back.services.PaymentService;
import com.stripe.exception.StripeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService){
        this.paymentService = paymentService;
    }

    @PostMapping("/intent")
    public String createIntent(@RequestBody PaymentRequestDTO paymentRequest) throws StripeException {
        return paymentService.createPaymentIntent(paymentRequest.amount());
    }

    @GetMapping("intent/confirm/{id}")
    public ResponseEntity<TicketResponseDTO> confirm(@PathVariable String id, @RequestParam Long sectorId) {
        try {
            TicketResponseDTO response = paymentService.confirmPayment(id,sectorId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}
