package com.cargopro.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Transporter Entity
 */
@Entity
@Table(name = "transporters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transporter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID transporterId;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private Double rating; // Rating from 0.0 to 5.0

    @ElementCollection
    @CollectionTable(name = "transporter_trucks", joinColumns = @JoinColumn(name = "transporter_id"))
    private List<TruckAvailability> availableTrucks = new ArrayList<>();
}
