package com.example.back.services;

import com.example.back.dto.EventRequestDTO;
import com.example.back.dto.SectorRequestDTO;
import com.example.back.model.Event;
import com.example.back.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class EventServiceTests {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;


    @Test
    @DisplayName("Deve criar um evento com data futura e associar seus setores")
    void createEvent_Success(){

        SectorRequestDTO sectorDTO = new SectorRequestDTO("Pista", 100, 50.0, 1L);
        EventRequestDTO dto = new EventRequestDTO(
                "Show Musical",
                "Descrição do Show",
                LocalDateTime.now().plusDays(10),
                "Estádio Municipal",
                1L,
                List.of(sectorDTO)

        );

        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Event created = eventService.createEvent(dto);

        assertNotNull(created);
        assertEquals("Show Musical", created.getName());
        assertEquals(1, created.getSectors().size());
        assertEquals("Pista", created.getSectors().get(0).getName());
        verify(eventRepository, times(1)).save(any(Event.class));

    }


    @Test
    @DisplayName("Deve recusar criação de evento com data no passado")
    void createEvent_PastDate_ThrowsException() {
        // Arrange
        EventRequestDTO dto = new EventRequestDTO(
                "Show Antigo",
                "Descrição",
                LocalDateTime.now().minusDays(1),
                "Local",
                1L,
                List.of()
        );

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> eventService.createEvent(dto));
        assertEquals("Não é possível criar eventos em datas passadas.", ex.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }
}
