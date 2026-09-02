package com.example.back.services;

import com.example.back.dto.TicketRequestDTO;
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
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {


    private final UserRepository userRepository;


    private final QRCodeService qrCodeService;


    private final SectorRepository sectorRepository;


    private final TicketRepository ticketRepository;

    public TicketService(UserRepository userRepository, QRCodeService qrCodeService, SectorRepository sectorRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
        this.sectorRepository = sectorRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public Ticket buyTicket(TicketRequestDTO data, User user) {
        Sector sector = sectorRepository.findById(data.sectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado"));

        if (sector.getCapacity() <= 0) {
            throw new BusinessRuleException("Setor esgotado! Não há mais ingressos disponiveis");
        }

        sector.setCapacity(sector.getCapacity() - 1);
        sectorRepository.save(sector);

        Ticket ticket = new Ticket();
        ticket.setUser(user);
        ticket.setSector(sector);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setQrCode(UUID.randomUUID().toString());

        return ticketRepository.save(ticket);
    }

    public List<Ticket> listByUser(User user) {
        return ticketRepository.findByUser(user);
    }

    public TicketResponseDTO toDTO(Ticket ticket) {
        try {
            String qrBase64 = qrCodeService.generateQRcodeBase64(ticket.getQrCode());

            var event = ticket.getSector().getEvent();
            var sector = ticket.getSector();

            return new TicketResponseDTO(
                    ticket.getQrCode(),
                    qrBase64,
                    event.getName(),
                    event.getDateTime(),
                    event.getLocation(),
                    sector.getName(),
                    ticket.getPrice()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter ticket para DTO", e);
        }
    }
}
