package com.example.back.controller;

import com.example.back.dto.SectorRequestDTO;
import com.example.back.model.Sector;
import com.example.back.services.SectorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sectors")
public class SectorController {

    @Autowired
    private SectorService sectorService;

    @GetMapping
    public ResponseEntity<List<Sector>> getAll(){
        return ResponseEntity.ok(sectorService.listAll());
    }

    @PostMapping
    public ResponseEntity<Sector> createSector(@RequestBody @Valid SectorRequestDTO data){
        Sector newSector = sectorService.createSector(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(newSector);

    }
}
