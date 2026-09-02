package com.example.back.dto;

public record ValidationErrorDTO(
        String field,
        String message
) {}
