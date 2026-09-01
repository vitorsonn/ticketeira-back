package com.example.back.services;

import com.example.back.dto.TicketRequestDTO;
import com.example.back.model.Sector;
import com.example.back.model.Ticket;
import com.example.back.model.TicketStatus;
import com.example.back.model.User;
import com.example.back.repository.SectorRepository;
import com.example.back.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
 public class TicketServiceTests {

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private QRCodeService qrCodeService;

    @InjectMocks
    private TicketService ticketService;

    @Test
    @DisplayName("Deve comprar ingresso com sucesso e decrementar a capacidade do setor")
    public void buyTicket_Success(){
        Long sectorId = 1L;
        TicketRequestDTO ticketRequestDTO = new TicketRequestDTO(sectorId);
        User user = new User();
        user.setEmail("user@example.com");

        Sector sector = new Sector();
        sector.setId(sectorId);
        sector.setCapacity(10);
        sector.setName("Pista");

        when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));
        when(sectorRepository.save(any(Sector.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Ticket ticket = ticketService.buyTicket(ticketRequestDTO, user);

        assertNotNull(ticket);
        assertEquals(user, ticket.getUser());
        assertEquals(sector, ticket.getSector());
        assertEquals(TicketStatus.ACTIVE, ticket.getStatus());
        assertEquals(9, sector.getCapacity());
        assertNotNull(ticket.getQrCode());

        verify(sectorRepository, times(1)).save(sector);
        verify(ticketRepository, times(1)).save(ticket);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o setor estiver esgotado")
    public void buyTicket_SectorSoldOut() {
        Long sectorId = 1L;
        TicketRequestDTO requestDTO = new TicketRequestDTO(sectorId);
        User user = new User();

        Sector sector = new Sector();
        sector.setId(sectorId);
        sector.setCapacity(0);

        when(sectorRepository.findById(sectorId)).thenReturn(Optional.of(sector));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.buyTicket(requestDTO, user);
        });

        assertEquals("Setor esgotado! Não há mais ingressos disponiveis", exception.getMessage());
        verify(ticketRepository, never()).save(any());

    }

    @Test
    @DisplayName("Deve lançar exceção quando o setor não for encontrado")
    void buyTicket_SectorNotFound() {
        // Arrange
        TicketRequestDTO requestDTO = new TicketRequestDTO(999L);
        User user = new User();

        when(sectorRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.buyTicket(requestDTO, user);
        });

        assertEquals("Setor nao encontrado", exception.getMessage());
        verify(ticketRepository, never()).save(any());
    }

}
