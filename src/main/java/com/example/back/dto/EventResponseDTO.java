package com.example.back.dto;

import com.example.back.model.Event;

import java.util.List;

public record EventResponseDTO(Long id, String name, String location, String description, List<SectorResponseDTO> sectors) {

    public EventResponseDTO(Event event){
        this(event.getId(), event.getName(), event.getLocation(), event.getDescription(),
                event.getSectors().stream().map(SectorResponseDTO::new).toList());
    }
}
