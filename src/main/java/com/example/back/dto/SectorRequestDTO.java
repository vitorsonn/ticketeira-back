package com.example.back.dto;

import com.example.back.model.Event;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SectorRequestDTO(@NotBlank String name,
                               @NotNull @Min(1) Integer capacity,
                               @NotNull @Positive Double preco,
                               @NotNull Long eventId) {
}
