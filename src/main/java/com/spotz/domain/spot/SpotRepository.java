package com.spotz.domain.spot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpotRepository extends JpaRepository<Spot, Long>, SpotRepositoryCustom {
    boolean existsBySourceId(String sourceId);
    Optional<Spot> findBySourceId(String sourceId);

    long countBySpotType(Spot.SpotType spotType);

}
