package com.example.back.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "events")

public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    @Column(nullable = false)
    private String location;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sector> sectors = new ArrayList<>();

    public void validateEventDate(){
        if(this.dateTime == null || this.dateTime.isBefore(LocalDateTime.now())){
            throw new IllegalArgumentException("Não é possível criar eventos em datas passadas.");
        }
    }

    public void addSector(Sector sector){
        if (this.sectors == null){
            this.sectors = new ArrayList<>();
        }
        this.sectors.add(sector);
        sector.setEvent(this);
    }

}
