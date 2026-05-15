package com.example.back.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record EventRequestDTO(String name,
                              String location,
                              @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
                              LocalDateTime dateTime,
                              String description,
                              Long Id,
                              List<SectorRequestDTO> sectors) {
}
