package com.example.back.services;

import com.example.back.dto.EventRequestDTO;
import com.example.back.dto.EventResponseDTO;
import com.example.back.exceptions.BusinessRuleException;
import com.example.back.exceptions.ResourceNotFoundException;
import com.example.back.mapper.EventMapper;
import com.example.back.model.Event;
import com.example.back.repository.EventRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventRepository repository;
    private final EventMapper eventMapper;

    public EventService(EventRepository repository, EventMapper eventMapper) {
        this.repository = repository;
        this.eventMapper = eventMapper;
    }

    @Transactional
    public Event createEvent(EventRequestDTO data) {
        if (data.dateTime().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Não é possível criar eventos em datas passadas.");
        }

        Event newEvent = eventMapper.toEntity(data);
        return repository.save(newEvent);
    }

    public List<Event> listAll() {
        return repository.findAll();
    }

    public EventResponseDTO getById(Long id) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado com o ID: " + id));

        return eventMapper.toResponseDTO(event);
    }

    public void deleteEvent(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Evento não encontrado com o ID: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public EventResponseDTO updateEvent(Long id, EventRequestDTO dto) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado com o ID: " + id));

        eventMapper.updateEntityFromDTO(dto, event);
        Event updatedEvent = repository.save(event);
        return eventMapper.toResponseDTO(updatedEvent);
    }
}
