package com.example.back.services;

import com.example.back.dto.UserRequestDTO;
import com.example.back.model.User;
import com.example.back.model.UserRole;
import com.example.back.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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

    @Autowired
    private UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        return repository.findByEmail(username)
                .orElseThrow(()-> new UsernameNotFoundException("Usuario não encontrado com o email" + username));
    }

    @Transactional
    public User register(UserRequestDTO data){
        if(repository.findByEmail(data.email()).isPresent()){
            throw new RuntimeException("Este e-mail já está cadastrado.");
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());

        User newUser = new User();
        newUser.setEmail(data.email());
        newUser.setPassword(encryptedPassword);
        newUser.setRole(UserRole.USER);

        if (data.email().equalsIgnoreCase("admin@ticketeira.com")) {
            newUser.setRole(UserRole.ADMIN);
        } else {
            newUser.setRole(UserRole.USER);
        }

        return repository.save(newUser);
    }

    @Value("${api.security.token.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String gerarToken(User user) {


        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }

    public String validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            System.err.println("Token inválido ou expirado: " + e.getMessage());
            return null;
        }
    }
}


