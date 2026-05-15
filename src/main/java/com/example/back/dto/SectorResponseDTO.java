package com.example.back.dto;

import com.example.back.model.Sector;

public record SectorResponseDTO(Long id, String name, Integer capacity, Double preco) {
    public SectorResponseDTO(Sector sector) {
        this(
                sector.getId(),
                sector.getName(),
                sector.getCapacity(),
                sector.getPreco()
        );
    }
}
