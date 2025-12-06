package com.cargopro.repository;

import com.cargopro.entity.Load;
import com.cargopro.enums.LoadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.UUID;

/**
 * Repository for Load entity
 * JpaRepository provides basic CRUD operations automatically
 * We can add custom query methods here if needed
 */
@Repository
public interface LoadRepository extends JpaRepository<Load, UUID>, JpaSpecificationExecutor<Load> {

    // Find all loads by status
    List<Load> findByStatus(LoadStatus status);

    // Find active loads (not cancelled or booked)
    List<Load> findByStatusNot(LoadStatus status);
}
