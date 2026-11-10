package com.example.back.mapper;

import com.example.back.dto.SectorRequestDTO;
import com.example.back.dto.SectorResponseDTO;
import com.example.back.model.Event;
import com.example.back.model.Sector;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SectorMapper {

    public Sector toEntity(SectorRequestDTO dto, Event event) {
        if (dto == null) {
            return null;
        }

        Sector sector = new Sector();
        sector.setName(dto.name());
        sector.setCapacity(dto.capacity());
        sector.setPreco(dto.preco());
        sector.setEvent(event);
        return sector;
    }

    public SectorResponseDTO toResponseDTO(Sector sector) {
        if (sector == null) {
            return null;
        }

        return new SectorResponseDTO(
                sector.getId(),
                sector.getName(),
                sector.getCapacity(),
                sector.getPreco()
        );
    }

    public List<SectorResponseDTO> toResponseDTOList(List<Sector> sectors) {
        if (sectors == null) {
            return List.of();
        }
        return sectors.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
