package com.cargopro.repository;

import com.cargopro.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Booking entity
 * JpaRepository provides basic CRUD operations automatically
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // Find bookings by load
    List<Booking> findByLoadLoadId(UUID loadId);

    // Find bookings by transporter
    List<Booking> findByTransporterTransporterId(UUID transporterId);
}
