package com.example.back.services;

import com.example.back.dto.SectorRequestDTO;
import com.example.back.exceptions.ResourceNotFoundException;
import com.example.back.model.Event;
import com.example.back.model.Sector;
import com.example.back.repository.EventRepository;
import com.example.back.repository.SectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SectorService {


    private final SectorRepository sectorRepository;


    private final EventRepository eventRepository;

    public SectorService(SectorRepository sectorRepository, EventRepository eventRepository) {
        this.sectorRepository = sectorRepository;
        this.eventRepository = eventRepository;
    }

    public Sector createSector(SectorRequestDTO data) {
        Event event = eventRepository.findById(data.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado com o ID: " + data.eventId()));

        Sector sector = new Sector();
        sector.setName(data.name());
        sector.setPreco(data.preco());
        sector.setCapacity(data.capacity());
        sector.setEvent(event);

        return sectorRepository.save(sector);
    }

    public List<Sector> listAll() {
        return sectorRepository.findAll();
    }
}
