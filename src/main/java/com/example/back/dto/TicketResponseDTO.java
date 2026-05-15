package com.example.back.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponseDTO(
        String ticketHash,
        String qrCodeBase64,
        String eventName,
        LocalDateTime eventDate,
        String location,
        String sector,
        BigDecimal price

) {}