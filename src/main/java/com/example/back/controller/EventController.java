package com.example.back.controller;


import com.example.back.dto.EventRequestDTO;
import com.example.back.dto.EventResponseDTO;
import com.example.back.services.EventService;
import jakarta.validation.Valid;
import com.example.back.model.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.back.repository.EventRepository;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {


    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAll(){
        return ResponseEntity.ok(eventService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Event> saveEvent(@RequestBody @Valid EventRequestDTO data){
      Event newEvent = eventService.createEvent(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(newEvent);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        eventService.deleteEvent(id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponseDTO> update(@PathVariable Long id, @RequestBody EventRequestDTO dto){
        EventResponseDTO updated = eventService.updateEvent(id, dto);
        return ResponseEntity.ok(updated);
    }





}
