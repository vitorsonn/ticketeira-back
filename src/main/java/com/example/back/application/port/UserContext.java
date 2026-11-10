package com.example.back.application.port;

import com.example.back.model.User;

import java.util.Optional;

public interface UserContext {

    Optional<User> getCurrentUser();

    User getRequiredCurrentUser();
}
