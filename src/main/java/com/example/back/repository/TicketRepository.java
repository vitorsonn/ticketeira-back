package com.example.back.repository;

import com.example.back.model.Ticket;
import com.example.back.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUser(User user);
    Optional<Ticket> findByPaymentIntentId(String paymentIntentId);
    Optional<Ticket> findByQrCode(String qrCode);
}
