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
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ConfirmPaymentUseCase {

    private final PaymentGateway paymentGateway;
    private final TicketRepository ticketRepository;
    private final SectorRepository sectorRepository;
    private final UserRepository userRepository;


    public ConfirmPaymentUseCase(PaymentGateway paymentGateway, TicketRepository ticketRepository, SectorRepository sectorRepository, UserRepository userRepository) {
        this.paymentGateway = paymentGateway;
        this.ticketRepository = ticketRepository;
        this.sectorRepository = sectorRepository;
        this.userRepository = userRepository;
    }


    @Transactional
    public Ticket execute(String paymentIntentId,
                                     Long sectorId,
                                     String userEmail){

        PaymentInfo payment = paymentGateway.getPayment(paymentIntentId);

        if(!"succeeded".equals(payment.status())){
            throw new BusinessRuleException("Pagamento ainda não aprovado");
        }

        var ticketExistente = ticketRepository.findByPaymentIntentId(paymentIntentId);

        if(ticketExistente.isPresent()){
            return ticketExistente.get();
        }

        Sector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado"));


        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));


        Ticket ticket = new Ticket();
        ticket.setPaymentIntentId(paymentIntentId);
        ticket.setSector(sector);
        ticket.setUser(user);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setPrice(payment.amount());
        ticket.setQrCode(UUID.randomUUID().toString());
        ticket.setStatus(TicketStatus.ACTIVE);
        return ticketRepository.save(ticket);
    }


}
