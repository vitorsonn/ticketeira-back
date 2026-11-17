package com.example.back.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Entity
@Table(name = "sectors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Double preco;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    public boolean hasAvailableCapacity(){
        return this.capacity != null && this.capacity > 0;
    }

    public void reserveCapacity(){
        if(!hasAvailableCapacity()){
            throw new IllegalStateException("Não há capacidade disponível para reservar neste setor.");
        }
        this.capacity--;
    }

    public void releaseCapacity(){
        if(this.capacity != null){
            this.capacity++;
        }
    }


}
