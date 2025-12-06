package com.cargopro.repository;

import com.cargopro.entity.Transporter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

/**
 * Repository for Transporter entity
 * JpaRepository provides basic CRUD operations automatically
 */
@Repository
public interface TransporterRepository extends JpaRepository<Transporter, UUID> {
}
