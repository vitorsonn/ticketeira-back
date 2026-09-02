package com.example.back.dto;

import java.time.Instant;
import java.util.List;

public record ValidationErrorsResponseDTO(
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path,
        List<ValidationErrorDTO> errors
) {}
