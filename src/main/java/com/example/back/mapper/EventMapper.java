package com.example.back.mapper;

import com.example.back.dto.EventRequestDTO;
import com.example.back.dto.EventResponseDTO;
import com.example.back.dto.SectorResponseDTO;
import com.example.back.model.Event;
import com.example.back.model.Sector;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class EventMapper {

    private final SectorMapper sectorMapper;

    public EventMapper(SectorMapper sectorMapper) {
        this.sectorMapper = sectorMapper;
    }

    public Event toEntity(EventRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Event event = new Event();
        event.setName(dto.name());
        event.setDescription(dto.description());
        event.setDateTime(dto.dateTime());
        event.setLocation(dto.location());

        if (dto.sectors() != null) {
            List<Sector> sectors = dto.sectors().stream()
                    .map(sectorDto -> sectorMapper.toEntity(sectorDto, event))
                    .toList();
            event.setSectors(sectors);
        }

        return event;
    }

    public EventResponseDTO toResponseDTO(Event event) {
        if (event == null) {
            return null;
        }

        List<SectorResponseDTO> sectorDtos = event.getSectors() != null
                ? sectorMapper.toResponseDTOList(event.getSectors())
                : List.of();

        return new EventResponseDTO(
                event.getId(),
                event.getName(),
                event.getLocation(),
                event.getDescription(),
                sectorDtos
        );
    }

    public List<EventResponseDTO> toResponseDTOList(List<Event> events) {
        if (events == null) {
            return List.of();
        }
        return events.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public void updateEntityFromDTO(EventRequestDTO dto, Event event) {
        if (dto == null || event == null) {
            return;
        }

        event.setName(dto.name());
        event.setDescription(dto.description());
        event.setDateTime(dto.dateTime());
        event.setLocation(dto.location());

        if (dto.sectors() != null) {
            List<Sector> currentSectors = event.getSectors();
            if (currentSectors == null) {
                currentSectors = new ArrayList<>();
                event.setSectors(currentSectors);
            }

            for (var sectorDto : dto.sectors()) {
                Optional<Sector> existing = currentSectors.stream()
                        .filter(s -> s.getName().equalsIgnoreCase(sectorDto.name()))
                        .findFirst();

                if (existing.isPresent()) {
                    Sector sector = existing.get();
                    sector.setCapacity(sectorDto.capacity());
                    sector.setPreco(sectorDto.preco());
                } else {
                    Sector newSector = sectorMapper.toEntity(sectorDto, event);
                    currentSectors.add(newSector);
                }
            }
        }
    }
}
