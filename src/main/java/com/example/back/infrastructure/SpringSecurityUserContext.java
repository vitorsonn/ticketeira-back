package com.example.back.infrastructure;


import com.example.back.application.port.UserContext;
import com.example.back.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringSecurityUserContext implements UserContext {

    @Override
    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Optional.empty();
        }

        if (auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return Optional.of(userDetails.getUser());
        }

        if (auth.getPrincipal() instanceof User user) {
            return Optional.of(user);
        }

        return Optional.empty();
    }

    @Override
    public User getRequiredCurrentUser() {
        return getCurrentUser().orElseThrow(() -> new AccessDeniedException("Usuário não autenticado"));
    }


}
