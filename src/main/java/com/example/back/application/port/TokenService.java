package com.example.back.application.port;

import com.example.back.model.User;

public interface TokenService {

    String generateToken(User user);

    String validateToken(String token);




}
