package com.example.back.mapper;

import com.example.back.application.port.QrCodeGenerator;
import com.example.back.dto.TicketResponseDTO;
import com.example.back.model.Event;
import com.example.back.model.Sector;
import com.example.back.model.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketMapperTest {

    @Mock
    private QrCodeGenerator qrCodeGenerator;

    private TicketMapper ticketMapper;

    @BeforeEach
    void setUp() {
        ticketMapper = new TicketMapper(qrCodeGenerator);
    }

    @Test
    @DisplayName("Deve converter Ticket para TicketResponseDTO com QR code em base64")
    void shouldMapTicketToResponseDTO() throws Exception {
        Event event = new Event();
        event.setName("Rock In Rio");
        event.setLocation("Parque Olímpico");
        LocalDateTime eventDate = LocalDateTime.now().plusDays(20);
        event.setDateTime(eventDate);

        Sector sector = new Sector();
        sector.setName("Palco Mundo");
        sector.setEvent(event);

        Ticket ticket = new Ticket();
        ticket.setQrCode("qr-uuid-test");
        ticket.setPrice(new BigDecimal("350.00"));
        ticket.setSector(sector);

        when(qrCodeGenerator.generateQrCode("qr-uuid-test")).thenReturn("base64-mock-img");

        TicketResponseDTO dto = ticketMapper.toResponseDTO(ticket);

        assertNotNull(dto);
        assertEquals("qr-uuid-test", dto.ticketHash());
        assertEquals("base64-mock-img", dto.qrCodeBase64());
        assertEquals("Rock In Rio", dto.eventName());
        assertEquals("Parque Olímpico", dto.location());
        assertEquals("Palco Mundo", dto.sector());
        assertEquals(new BigDecimal("350.00"), dto.price());
    }

    @Test
    @DisplayName("Deve retornar null se ticket for nulo")
    void shouldReturnNullWhenTicketIsNull() {
        assertNull(ticketMapper.toResponseDTO(null));
        assertTrue(ticketMapper.toResponseDTOList(null).isEmpty());
    }
}
