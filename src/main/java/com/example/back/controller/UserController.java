package com.example.back.controller;
import com.example.back.application.port.TokenService;
import com.example.back.dto.AuthenticationDTO;
import com.example.back.dto.LoginResponseDTO;
import com.example.back.dto.UserRequestDTO;
import com.example.back.infrastructure.UserDetailsImpl;
import com.example.back.model.User;
import com.example.back.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;


@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/users")
public class UserController {




    private final UserService userService;

    private final TokenService tokenService;

    private final AuthenticationManager authenticationManager;

    public UserController(UserService userService, AuthenticationManager authenticationManager, TokenService tokenService) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }


    @PostMapping("/register")
    public ResponseEntity<?> createUser(@RequestBody @Valid UserRequestDTO data){
        this.userService.register(data);

        return ResponseEntity.ok().body(Collections.singletonMap("message", "Usuário cadastrado com sucesso"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthenticationDTO data){
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());

        var auth = this.authenticationManager.authenticate(usernamePassword);

        var userDetails = (UserDetailsImpl) auth.getPrincipal();
        var user = userDetails.getUser();

        String token = tokenService.generateToken(user);

        LoginResponseDTO response = new LoginResponseDTO(token, user.getRole().name(), user.getEmail());

        return ResponseEntity.ok(response);
    }


}
