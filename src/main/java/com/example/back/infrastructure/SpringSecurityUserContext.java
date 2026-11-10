package com.example.back.infrastructure;


import com.example.back.application.port.UserContext;
import com.example.back.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringSecurityUserContext implements UserContext {

    @Override
    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User)) {
            return Optional.empty();
        }

        return Optional.of((User) auth.getPrincipal());

    }

    @Override
    public User getRequiredCurrentUser() {
        return getCurrentUser().orElseThrow(() -> new RuntimeException("Usuário não autenticado"));
    }


}
