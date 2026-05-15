package com.example.back.dto;

import com.example.back.model.UserRole;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(String email, @Size(min = 6) String password) {
}
