package com.example.back.services;

import com.example.back.dto.SectorRequestDTO;
import com.example.back.dto.SectorResponseDTO;
import com.example.back.exceptions.ResourceNotFoundException;
import com.example.back.mapper.SectorMapper;
import com.example.back.model.Event;
import com.example.back.model.Sector;
import com.example.back.repository.EventRepository;
import com.example.back.repository.SectorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SectorService {

    private final SectorRepository sectorRepository;
    private final EventRepository eventRepository;
    private final SectorMapper sectorMapper;

    public SectorService(SectorRepository sectorRepository,
                         EventRepository eventRepository,
                         SectorMapper sectorMapper) {
        this.sectorRepository = sectorRepository;
        this.eventRepository = eventRepository;
        this.sectorMapper = sectorMapper;
    }

    public Sector createSector(SectorRequestDTO data) {
        Event event = eventRepository.findById(data.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado com o ID: " + data.eventId()));

        Sector sector = sectorMapper.toEntity(data, event);
        return sectorRepository.save(sector);
    }

    public List<Sector> listAll() {
        return sectorRepository.findAll();
    }
}
