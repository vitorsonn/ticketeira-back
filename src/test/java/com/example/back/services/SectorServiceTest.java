package com.example.back.services;

import com.example.back.dto.SectorRequestDTO;
import com.example.back.exceptions.ResourceNotFoundException;
import com.example.back.model.Event;
import com.example.back.model.Sector;
import com.example.back.repository.EventRepository;
import com.example.back.repository.SectorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectorServiceTest {

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private SectorService sectorService;

    @Test
    @DisplayName("Deve criar setor com sucesso associado a um evento existente")
    void createSector_Success() {
        // Arrange
        Long eventId = 1L;
        SectorRequestDTO requestDTO = new SectorRequestDTO("Camarote", 50, 250.0, eventId);

        Event event = new Event();
        event.setId(eventId);
        event.setName("Festival de Verão");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(sectorRepository.save(any(Sector.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Sector result = sectorService.createSector(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Camarote", result.getName());
        assertEquals(50, result.getCapacity());
        assertEquals(250.0, result.getPreco());
        assertEquals(event, result.getEvent());
        verify(eventRepository, times(1)).findById(eventId);
        verify(sectorRepository, times(1)).save(any(Sector.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar criar setor para evento inexistente")
    void createSector_EventNotFound_ThrowsException() {
        // Arrange
        Long eventId = 999L;
        SectorRequestDTO requestDTO = new SectorRequestDTO("Pista", 100, 80.0, eventId);

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            sectorService.createSector(requestDTO);
        });

        assertEquals("Evento não encontrado com o ID: 999", exception.getMessage());
        verify(sectorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar todos os setores")
    void listAll_Success() {
        // Arrange
        Sector sector1 = new Sector();
        sector1.setName("VIP");
        Sector sector2 = new Sector();
        sector2.setName("Pista");

        when(sectorRepository.findAll()).thenReturn(List.of(sector1, sector2));

        // Act
        List<Sector> result = sectorService.listAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(sectorRepository, times(1)).findAll();
    }
}
