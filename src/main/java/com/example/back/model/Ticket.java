package com.example.back.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Ticket {

    public static Ticket createNew(User user, Sector sector, BigDecimal price, String paymentIntentId) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (sector == null) {
            throw new IllegalArgumentException("Sector cannot be null");
        }
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        if (paymentIntentId == null) {
            throw new IllegalArgumentException("Payment intent ID cannot be null");
        }

        sector.reserveCapacity(); // Reduz a capacidade disponível do setor

        Ticket ticket = new Ticket();
        ticket.setUser(user);
        ticket.setSector(sector);
        ticket.setPrice(price != null ? price : BigDecimal.valueOf(sector.getPreco()));
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setQrCode(UUID.randomUUID().toString());
        ticket.setPaymentIntentId(paymentIntentId);
        return ticket;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String paymentIntentId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    private BigDecimal price;

    private LocalDateTime purchaseDate;

    private String qrCode; // Um código único para validar na entrada

    @Enumerated(EnumType.STRING)
    private TicketStatus status; // Ex: ACTIVE, USED, CANCELLED

    public void cancel() {
        if (this.status == TicketStatus.CANCELLED) {
            throw new IllegalStateException("O ticket já está cancelado.");
        }

        if (this.status == TicketStatus.USED) {
            throw new IllegalStateException("Não é possível cancelar um ticket que já foi usado.");
        }
        this.status = TicketStatus.CANCELLED;
        this.sector.releaseCapacity(); // Libera a capacidade do setor
    }

    public void markAsUsed() {
        if (this.status != TicketStatus.ACTIVE) {
            throw new IllegalStateException("Apenas ingressos ativos podem ser utilizados na entrada.");
        }
        this.status = TicketStatus.USED;
    }


}
