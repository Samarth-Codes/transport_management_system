package com.cargopro.repository;

import com.cargopro.entity.Bid;
import com.cargopro.enums.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID>, JpaSpecificationExecutor<Bid> {

    List<Bid> findByLoadLoadId(UUID loadId);

    List<Bid> findByTransporterTransporterId(UUID transporterId);

    List<Bid> findByStatus(BidStatus status);
}
