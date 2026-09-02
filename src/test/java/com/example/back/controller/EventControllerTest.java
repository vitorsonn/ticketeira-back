package com.example.back.controller;

import com.example.back.config.ControllerAdvice;
import com.example.back.config.SecurityFilter;
import com.example.back.dto.EventRequestDTO;
import com.example.back.dto.EventResponseDTO;
import com.example.back.dto.SectorResponseDTO;
import com.example.back.exceptions.BusinessRuleException;
import com.example.back.exceptions.ResourceNotFoundException;
import com.example.back.model.Event;
import com.example.back.services.EventService;
import com.example.back.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@Import(ControllerAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("GET /events")
    class GetAllTests {

        @Test
        @DisplayName("Deve listar todos os eventos com sucesso e retornar HTTP 200")
        void getAll_Success() throws Exception {
            Event event1 = new Event();
            event1.setId(1L);
            event1.setName("Rock in Rio");

            Event event2 = new Event();
            event2.setId(2L);
            event2.setName("Lollapalooza");

            when(eventService.listAll()).thenReturn(List.of(event1, event2));

            mockMvc.perform(get("/events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("Rock in Rio"))
                    .andExpect(jsonPath("$[1].name").value("Lollapalooza"));

            verify(eventService, times(1)).listAll();
        }
    }

    @Nested
    @DisplayName("GET /events/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("Deve retornar detalhes do evento por ID e retornar HTTP 200")
        void getById_Success() throws Exception {
            Long eventId = 1L;
            SectorResponseDTO sectorDTO = new SectorResponseDTO(1L, "Pista", 500, 100.0);
            EventResponseDTO responseDTO = new EventResponseDTO(eventId, "Festival de Jazz", "Parque", "Desc", List.of(sectorDTO));

            when(eventService.getById(eventId)).thenReturn(responseDTO);

            mockMvc.perform(get("/events/{id}", eventId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(eventId))
                    .andExpect(jsonPath("$.name").value("Festival de Jazz"))
                    .andExpect(jsonPath("$.location").value("Parque"))
                    .andExpect(jsonPath("$.sectors", hasSize(1)))
                    .andExpect(jsonPath("$.sectors[0].name").value("Pista"));

            verify(eventService, times(1)).getById(eventId);
        }

        @Test
        @DisplayName("Deve retornar HTTP 404 quando o evento não for encontrado")
        void getById_NotFound_Returns404() throws Exception {
            Long eventId = 999L;
            when(eventService.getById(eventId))
                    .thenThrow(new ResourceNotFoundException("Evento não encontrado com o ID: " + eventId));

            mockMvc.perform(get("/events/{id}", eventId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Recurso não encontrado"))
                    .andExpect(jsonPath("$.message").value("Evento não encontrado com o ID: 999"))
                    .andExpect(jsonPath("$.path").value("/events/999"));

            verify(eventService, times(1)).getById(eventId);
        }
    }

    @Nested
    @DisplayName("POST /events")
    class CreateEventTests {

        @Test
        @DisplayName("Deve criar um evento com sucesso e retornar HTTP 201")
        void createEvent_Success() throws Exception {
            String jsonPayload = """
                {
                    "name": "Carnaval 2027",
                    "location": "Sambódromo",
                    "dateTime": "2027-02-20T20:00:00",
                    "description": "Grande festa",
                    "sectors": [
                        {
                            "name": "VIP",
                            "capacity": 100,
                            "preco": 200.0,
                            "eventId": 1
                        }
                    ]
                }
                """;

            Event createdEvent = new Event();
            createdEvent.setId(1L);
            createdEvent.setName("Carnaval 2027");
            createdEvent.setLocation("Sambódromo");

            when(eventService.createEvent(any(EventRequestDTO.class))).thenReturn(createdEvent);

            mockMvc.perform(post("/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Carnaval 2027"))
                    .andExpect(jsonPath("$.location").value("Sambódromo"));

            verify(eventService, times(1)).createEvent(any(EventRequestDTO.class));
        }

        @Test
        @DisplayName("Deve retornar HTTP 400 com erro padronizado ao tentar criar evento com data no passado")
        void createEvent_PastDate_Returns400() throws Exception {
            String jsonPayload = """
                {
                    "name": "Show Passado",
                    "location": "Arena",
                    "dateTime": "2020-01-01T20:00:00",
                    "description": "Desc",
                    "sectors": []
                }
                """;

            when(eventService.createEvent(any(EventRequestDTO.class)))
                    .thenThrow(new BusinessRuleException("Não é possível criar eventos em datas passadas."));

            mockMvc.perform(post("/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Regra de negócio violada"))
                    .andExpect(jsonPath("$.message").value("Não é possível criar eventos em datas passadas."))
                    .andExpect(jsonPath("$.path").value("/events"));

            verify(eventService, times(1)).createEvent(any(EventRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("PUT /events/{id}")
    class UpdateEventTests {

        @Test
        @DisplayName("Deve atualizar evento com sucesso e retornar HTTP 200")
        void updateEvent_Success() throws Exception {
            Long eventId = 1L;
            String jsonPayload = """
                {
                    "name": "Nome Atualizado",
                    "location": "Novo Local",
                    "dateTime": "2027-05-10T19:00:00",
                    "description": "Nova Desc",
                    "sectors": []
                }
                """;

            EventResponseDTO responseDTO = new EventResponseDTO(eventId, "Nome Atualizado", "Novo Local", "Nova Desc", List.of());

            when(eventService.updateEvent(eq(eventId), any(EventRequestDTO.class))).thenReturn(responseDTO);

            mockMvc.perform(put("/events/{id}", eventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(eventId))
                    .andExpect(jsonPath("$.name").value("Nome Atualizado"))
                    .andExpect(jsonPath("$.location").value("Novo Local"));

            verify(eventService, times(1)).updateEvent(eq(eventId), any(EventRequestDTO.class));
        }

        @Test
        @DisplayName("Deve retornar HTTP 404 ao tentar atualizar evento inexistente")
        void updateEvent_NotFound_Returns404() throws Exception {
            Long eventId = 999L;
            String jsonPayload = """
                {
                    "name": "Nome",
                    "location": "Local",
                    "dateTime": "2027-05-10T19:00:00",
                    "description": "Desc",
                    "sectors": []
                }
                """;

            when(eventService.updateEvent(eq(eventId), any(EventRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Evento não encontrado com o ID: " + eventId));

            mockMvc.perform(put("/events/{id}", eventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Recurso não encontrado"))
                    .andExpect(jsonPath("$.message").value("Evento não encontrado com o ID: 999"));

            verify(eventService, times(1)).updateEvent(eq(eventId), any(EventRequestDTO.class));
        }
    }

    @Nested
    @DisplayName("DELETE /events/{id}")
    class DeleteEventTests {

        @Test
        @DisplayName("Deve deletar evento com sucesso e retornar HTTP 204 No Content")
        void deleteEvent_Success() throws Exception {
            Long eventId = 1L;
            doNothing().when(eventService).deleteEvent(eventId);

            mockMvc.perform(delete("/events/{id}", eventId))
                    .andExpect(status().isNoContent());

            verify(eventService, times(1)).deleteEvent(eventId);
        }

        @Test
        @DisplayName("Deve retornar HTTP 404 ao tentar deletar evento inexistente")
        void deleteEvent_NotFound_Returns404() throws Exception {
            Long eventId = 999L;
            doThrow(new ResourceNotFoundException("Evento não encontrado com o ID: " + eventId))
                    .when(eventService).deleteEvent(eventId);

            mockMvc.perform(delete("/events/{id}", eventId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Recurso não encontrado"))
                    .andExpect(jsonPath("$.message").value("Evento não encontrado com o ID: 999"));

            verify(eventService, times(1)).deleteEvent(eventId);
        }
    }
}
