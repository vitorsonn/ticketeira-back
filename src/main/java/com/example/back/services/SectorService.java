package com.example.back.services;


import com.example.back.dto.SectorRequestDTO;
import com.example.back.model.Event;
import com.example.back.model.Sector;
import com.example.back.repository.EventRepository;
import com.example.back.repository.SectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SectorService {

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private EventRepository eventRepository;

    public Sector createSector(SectorRequestDTO data){

     Event event = eventRepository.findById(data.eventId())
             .orElseThrow(() -> new RuntimeException("Evento não encontrado."));

        Sector sector = new Sector();
        sector.setName(data.name());
        sector.setPreco(data.preco());
        sector.setCapacity(data.capacity());
        sector.setEvent(event);

        return sectorRepository.save(sector);
    }

    public List<Sector> listAll(){
        return sectorRepository.findAll();
    }

    }

