package com.example.back.controller;

import com.example.back.application.port.UserContext;
import com.example.back.dto.TicketRequestDTO;
import com.example.back.dto.TicketResponseDTO;
import com.example.back.model.Ticket;
import com.example.back.model.User;
import com.example.back.services.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {


    private final TicketService ticketService;

    private final UserContext userContext;

    public TicketController(TicketService ticketService, UserContext userContext) {
        this.ticketService = ticketService;
        this.userContext = userContext;
    }

    @PostMapping
    public ResponseEntity<?> buy(@RequestBody @Valid TicketRequestDTO data) {

        User user = userContext.getRequiredCurrentUser();

        Ticket newTicket = ticketService.buyTicket(data, user);
        TicketResponseDTO response = ticketService.toDTO(newTicket);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<?> getMyTickets() {
        User user = userContext.getRequiredCurrentUser();
        List<Ticket> tickets = ticketService.listByUser(user);

        List<TicketResponseDTO> response = tickets.stream()
                .map(ticketService::toDTO)
                .toList();


        return ResponseEntity.ok(response);
    }
}
