package com.example.back.services;

import com.example.back.dto.TicketRequestDTO;
import com.example.back.dto.TicketResponseDTO;
import com.example.back.exceptions.BusinessRuleException;
import com.example.back.exceptions.ResourceNotFoundException;
import com.example.back.mapper.TicketMapper;
import com.example.back.model.Sector;
import com.example.back.model.Ticket;
import com.example.back.model.TicketStatus;
import com.example.back.model.User;
import com.example.back.repository.SectorRepository;
import com.example.back.repository.TicketRepository;
import com.example.back.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final UserRepository userRepository;
    private final SectorRepository sectorRepository;
    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;

    public TicketService(UserRepository userRepository,
                         SectorRepository sectorRepository,
                         TicketRepository ticketRepository,
                         TicketMapper ticketMapper) {
        this.userRepository = userRepository;
        this.sectorRepository = sectorRepository;
        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
    }

    @Transactional
    public Ticket buyTicket(TicketRequestDTO data, User user) {
        Sector sector = sectorRepository.findById(data.sectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado"));

        String paymentIdentifier = "DIRECT_" + UUID.randomUUID();

        Ticket ticket = Ticket.createNew(user, sector, BigDecimal.valueOf(sector.getPreco()), paymentIdentifier);
        sectorRepository.save(sector);
        return ticketRepository.save(ticket);
    }
    public List<Ticket> listByUser(User user) {
        return ticketRepository.findByUser(user);
    }

    public TicketResponseDTO toDTO(Ticket ticket) {
        return ticketMapper.toResponseDTO(ticket);
    }
}
