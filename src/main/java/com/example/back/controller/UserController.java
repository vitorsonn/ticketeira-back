package com.example.back.controller;
import com.example.back.dto.AuthenticationDTO;
import com.example.back.dto.LoginResponseDTO;
import com.example.back.dto.UserRequestDTO;
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


    private final AuthenticationManager authenticationManager;

    public UserController(UserService userService, AuthenticationManager authenticationManager) {
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

        var user = (User) auth.getPrincipal();

        assert user != null;
        String token = userService.gerarToken(user);

        LoginResponseDTO response = new LoginResponseDTO(token, user.getRole().name(), user.getEmail())
;

        return ResponseEntity.ok(response);

    }


}
