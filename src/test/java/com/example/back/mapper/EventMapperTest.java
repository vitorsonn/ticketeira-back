package com.example.back.mapper;

import com.example.back.dto.EventRequestDTO;
import com.example.back.dto.EventResponseDTO;
import com.example.back.dto.SectorRequestDTO;
import com.example.back.model.Event;
import com.example.back.model.Sector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventMapperTest {

    private EventMapper eventMapper;

    @BeforeEach
    void setUp() {
        SectorMapper sectorMapper = new SectorMapper();
        eventMapper = new EventMapper(sectorMapper);
    }

    @Test
    @DisplayName("Deve converter EventRequestDTO para entidade Event incluindo setores")
    void shouldMapDtoToEntity() {
        LocalDateTime now = LocalDateTime.now();
        SectorRequestDTO sectorDTO = new SectorRequestDTO("Pista", 50, 40.0, null);
        EventRequestDTO dto = new EventRequestDTO(
                "Concerto",
                "Estádio",
                now,
                "Descrição",
                null,
                List.of(sectorDTO)
        );

        Event event = eventMapper.toEntity(dto);

        assertNotNull(event);
        assertEquals("Concerto", event.getName());
        assertEquals("Estádio", event.getLocation());
        assertEquals("Descrição", event.getDescription());
        assertEquals(now, event.getDateTime());
        assertNotNull(event.getSectors());
        assertEquals(1, event.getSectors().size());
        assertEquals("Pista", event.getSectors().get(0).getName());
    }

    @Test
    @DisplayName("Deve converter Event para EventResponseDTO")
    void shouldMapEntityToResponseDTO() {
        Event event = new Event();
        event.setId(5L);
        event.setName("Show");
        event.setLocation("Arena");
        event.setDescription("Desc");
        event.setSectors(List.of());

        EventResponseDTO dto = eventMapper.toResponseDTO(event);

        assertNotNull(dto);
        assertEquals(5L, dto.id());
        assertEquals("Show", dto.name());
        assertEquals("Arena", dto.location());
        assertEquals("Desc", dto.description());
        assertTrue(dto.sectors().isEmpty());
    }

    @Test
    @DisplayName("Deve atualizar entidade Event a partir do DTO")
    void shouldUpdateEntityFromDTO() {
        Event event = new Event();
        event.setName("Nome Antigo");
        event.setLocation("Local Antigo");
        event.setSectors(new ArrayList<>());

        Sector existingSector = new Sector();
        existingSector.setName("VIP");
        existingSector.setCapacity(20);
        existingSector.setPreco(100.0);
        event.getSectors().add(existingSector);

        LocalDateTime newTime = LocalDateTime.now().plusDays(5);
        SectorRequestDTO updateSectorDTO = new SectorRequestDTO("VIP", 30, 120.0, null);
        SectorRequestDTO newSectorDTO = new SectorRequestDTO("Pista", 50, 60.0, null);

        EventRequestDTO dto = new EventRequestDTO(
                "Nome Novo",
                "Local Novo",
                newTime,
                "Nova Desc",
                null,
                List.of(updateSectorDTO, newSectorDTO)
        );

        eventMapper.updateEntityFromDTO(dto, event);

        assertEquals("Nome Novo", event.getName());
        assertEquals("Local Novo", event.getLocation());
        assertEquals("Nova Desc", event.getDescription());
        assertEquals(newTime, event.getDateTime());
        assertEquals(2, event.getSectors().size());
        assertEquals(30, existingSector.getCapacity());
        assertEquals(120.0, existingSector.getPreco());
    }
}
