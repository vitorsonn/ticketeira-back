package com.example.back.services;

import com.example.back.dto.EventRequestDTO;
import com.example.back.dto.EventResponseDTO;
import com.example.back.model.Event;
import com.example.back.model.Sector;
import com.example.back.repository.EventRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.spi.ToolProvider.findFirst;

@Service
public class EventService {

    @Autowired
    private EventRepository repository;

    @Transactional
    public Event createEvent(EventRequestDTO data) {
        if (data.dateTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Não é possível criar eventos em datas passadas.");
        }

        Event newEvent = new Event();
        newEvent.setName(data.name());
        newEvent.setDescription(data.description());
        newEvent.setDateTime(data.dateTime());
        newEvent.setLocation(data.location());

        if (data.sectors() != null) {
            List<Sector> sectors = data.sectors().stream().map(sectorDto -> {
                Sector sector = new Sector();
                sector.setName(sectorDto.name());
                sector.setCapacity(sectorDto.capacity());
                sector.setPreco(sectorDto.preco());
                sector.setEvent(newEvent);
                return sector;
            }).toList();

            newEvent.setSectors(sectors);
        }

        return repository.save(newEvent);

    }

    public List<Event> listAll(){
        return repository.findAll();
    }

    public EventResponseDTO getById(Long id) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        return new EventResponseDTO(event);
    }

    public void deleteEvent(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Evento não encontrado com o ID: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public EventResponseDTO updateEvent(Long id, EventRequestDTO dto) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado com o ID: " + id));

        event.setName(dto.name());
        event.setDescription(dto.description());
        event.setDateTime(dto.dateTime());
        event.setLocation(dto.location());

        if (dto.sectors() != null) {
            List<Sector> currentSectors = event.getSectors();

            dto.sectors().forEach(sectorDto -> {
                Optional<Sector> existingSector = currentSectors.stream()
                        .filter(s -> s.getName().equalsIgnoreCase(sectorDto.name()))
                        .findFirst();

                if (existingSector.isPresent()) {

                    Sector sector = existingSector.get();
                    sector.setCapacity(sectorDto.capacity());
                    sector.setPreco(sectorDto.preco());
                } else {
                    Sector newSector = new Sector();
                    newSector.setName(sectorDto.name());
                    newSector.setCapacity(sectorDto.capacity());
                    newSector.setPreco(sectorDto.preco());
                    newSector.setEvent(event);
                    currentSectors.add(newSector);
                }
            });
        }

        Event updatedEvent = repository.save(event);
        return new EventResponseDTO(updatedEvent);
    }



}
