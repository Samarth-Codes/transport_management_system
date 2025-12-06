package com.cargopro.entity;

import com.cargopro.enums.LoadStatus;
import com.cargopro.enums.WeightUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loads")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Load {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID loadId;

    @Column(nullable = false)
    private String shipperId;

    @Column(nullable = false)
    private String loadingCity;

    @Column(nullable = false)
    private String unloadingCity;

    @Column(nullable = false)
    private String productType;

    @Column(nullable = false)
    private String truckType;

    @Column(nullable = false)
    private int noOfTrucks;

    @Column(nullable = false)
    private int remainingTrucks;

    @Column(nullable = false)
    private double weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeightUnit weightUnit;

    @Column
    private String comment; // Optional comment field often useful

    @Column(nullable = false)
    private LocalDateTime loadingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoadStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime datePosted;

    @OneToMany(mappedBy = "load", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bid> bids = new ArrayList<>();

    @OneToMany(mappedBy = "load", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (datePosted == null) {
            datePosted = LocalDateTime.now();
        }
    }
}
