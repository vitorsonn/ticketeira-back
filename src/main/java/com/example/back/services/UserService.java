package com.example.back.services;

import com.example.back.dto.UserRequestDTO;
import com.example.back.exceptions.BusinessRuleException;
import com.example.back.model.User;
import com.example.back.model.UserRole;
import com.example.back.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class UserService implements UserDetailsService {

    private final long EXPIRATION_TIME = 86400000;


    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario não encontrado com o email: " + username));
    }

    @Transactional
    public User register(UserRequestDTO data) {
        if (repository.findByEmail(data.email()).isPresent()) {
            throw new BusinessRuleException("Este e-mail já está cadastrado.");
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());

        User newUser = new User();
        newUser.setEmail(data.email());
        newUser.setPassword(encryptedPassword);

        if (data.email().equalsIgnoreCase("admin@ticketeira.com")) {
            newUser.setRole(UserRole.ADMIN);
        } else {
            newUser.setRole(UserRole.USER);
        }

        return repository.save(newUser);
    }

}
