package com.example.back.controller;

import com.example.back.config.ControllerAdvice;
import com.example.back.config.SecurityFilter;
import com.example.back.dto.SectorRequestDTO;
import com.example.back.exceptions.ResourceNotFoundException;
import com.example.back.model.Event;
import com.example.back.model.Sector;
import com.example.back.services.SectorService;
import com.example.back.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SectorController.class)
@Import(ControllerAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class SectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SectorService sectorService;

    @MockitoBean
    private SecurityFilter securityFilter;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("GET /sectors")
    class GetAllTests {

        @Test
        @DisplayName("Deve listar todos os setores e retornar HTTP 200")
        void getAll_Success() throws Exception {
            Sector sector1 = new Sector();
            sector1.setId(1L);
            sector1.setName("VIP");

            Sector sector2 = new Sector();
            sector2.setId(2L);
            sector2.setName("Pista");

            when(sectorService.listAll()).thenReturn(List.of(sector1, sector2));

            mockMvc.perform(get("/sectors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("VIP"))
                    .andExpect(jsonPath("$[1].name").value("Pista"));

            verify(sectorService, times(1)).listAll();
        }
    }

    @Nested
    @DisplayName("POST /sectors")
    class CreateSectorTests {

        @Test
        @DisplayName("Deve criar setor com sucesso e retornar HTTP 201")
        void createSector_Success() throws Exception {
            SectorRequestDTO requestDTO = new SectorRequestDTO("Camarote", 50, 150.0, 1L);

            Event event = new Event();
            event.setId(1L);

            Sector createdSector = new Sector();
            createdSector.setId(10L);
            createdSector.setName("Camarote");
            createdSector.setCapacity(50);
            createdSector.setPreco(150.0);
            createdSector.setEvent(event);

            when(sectorService.createSector(any(SectorRequestDTO.class))).thenReturn(createdSector);

            mockMvc.perform(post("/sectors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.name").value("Camarote"))
                    .andExpect(jsonPath("$.capacity").value(50))
                    .andExpect(jsonPath("$.preco").value(150.0));

            verify(sectorService, times(1)).createSector(any(SectorRequestDTO.class));
        }

        @Test
        @DisplayName("Deve retornar HTTP 404 quando o evento associado ao setor não for encontrado")
        void createSector_EventNotFound_Returns404() throws Exception {
            SectorRequestDTO requestDTO = new SectorRequestDTO("Camarote", 50, 150.0, 999L);

            when(sectorService.createSector(any(SectorRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Evento não encontrado com o ID: 999"));

            mockMvc.perform(post("/sectors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Recurso não encontrado"))
                    .andExpect(jsonPath("$.message").value("Evento não encontrado com o ID: 999"))
                    .andExpect(jsonPath("$.path").value("/sectors"));

            verify(sectorService, times(1)).createSector(any(SectorRequestDTO.class));
        }

        @Test
        @DisplayName("Deve retornar HTTP 400 quando campos obrigatórios forem inválidos")
        void createSector_InvalidDTO_Returns400() throws Exception {
            // Nome vazio, capacidade zero, preço negativo
            SectorRequestDTO invalidDTO = new SectorRequestDTO("", 0, -10.0, null);

            mockMvc.perform(post("/sectors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Erro de validação"));

            verify(sectorService, never()).createSector(any());
        }
    }
}
