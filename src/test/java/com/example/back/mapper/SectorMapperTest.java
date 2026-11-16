package com.example.back.mapper;

import com.example.back.dto.SectorRequestDTO;
import com.example.back.dto.SectorResponseDTO;
import com.example.back.model.Event;
import com.example.back.model.Sector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SectorMapperTest {

    private SectorMapper sectorMapper;

    @BeforeEach
    void setUp() {
        sectorMapper = new SectorMapper();
    }

    @Test
    @DisplayName("Deve converter SectorRequestDTO e Event para entidade Sector")
    void shouldMapDtoToEntity() {
        Event event = new Event();
        event.setId(1L);
        event.setName("Festival");

        SectorRequestDTO dto = new SectorRequestDTO("VIP", 100, 150.0, 1L);

        Sector sector = sectorMapper.toEntity(dto, event);

        assertNotNull(sector);
        assertEquals("VIP", sector.getName());
        assertEquals(100, sector.getCapacity());
        assertEquals(150.0, sector.getPreco());
        assertEquals(event, sector.getEvent());
    }

    @Test
    @DisplayName("Deve converter entidade Sector para SectorResponseDTO")
    void shouldMapEntityToResponseDTO() {
        Sector sector = new Sector();
        sector.setId(10L);
        sector.setName("Pista");
        sector.setCapacity(200);
        sector.setPreco(80.0);

        SectorResponseDTO dto = sectorMapper.toResponseDTO(sector);

        assertNotNull(dto);
        assertEquals(10L, dto.id());
        assertEquals("Pista", dto.name());
        assertEquals(200, dto.capacity());
        assertEquals(80.0, dto.preco());
    }

    @Test
    @DisplayName("Deve converter lista de entidades Sector para lista de SectorResponseDTO")
    void shouldMapEntityListToResponseDTOList() {
        Sector sector1 = new Sector();
        sector1.setId(1L);
        sector1.setName("Setor 1");

        Sector sector2 = new Sector();
        sector2.setId(2L);
        sector2.setName("Setor 2");

        List<SectorResponseDTO> dtos = sectorMapper.toResponseDTOList(List.of(sector1, sector2));

        assertEquals(2, dtos.size());
        assertEquals("Setor 1", dtos.get(0).name());
        assertEquals("Setor 2", dtos.get(1).name());
    }

    @Test
    @DisplayName("Deve retornar null quando passar nulo para mapeamento")
    void shouldHandleNull() {
        assertNull(sectorMapper.toEntity(null, null));
        assertNull(sectorMapper.toResponseDTO(null));
        assertTrue(sectorMapper.toResponseDTOList(null).isEmpty());
    }
}
