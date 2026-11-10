package com.example.back.mapper;

import com.example.back.application.port.QrCodeGenerator;
import com.example.back.dto.TicketResponseDTO;
import com.example.back.model.Ticket;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketMapper {

    private final QrCodeGenerator qrCodeGenerator;

    public TicketMapper(QrCodeGenerator qrCodeGenerator) {
        this.qrCodeGenerator = qrCodeGenerator;
    }

    public TicketResponseDTO toResponseDTO(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        try {
            String qrBase64 = qrCodeGenerator.generateQrCode(ticket.getQrCode());

            var sector = ticket.getSector();
            var event = sector != null ? sector.getEvent() : null;

            return new TicketResponseDTO(
                    ticket.getQrCode(),
                    qrBase64,
                    event != null ? event.getName() : null,
                    event != null ? event.getDateTime() : null,
                    event != null ? event.getLocation() : null,
                    sector != null ? sector.getName() : null,
                    ticket.getPrice()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter ticket para DTO", e);
        }
    }

    public List<TicketResponseDTO> toResponseDTOList(List<Ticket> tickets) {
        if (tickets == null) {
            return List.of();
        }
        return tickets.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
