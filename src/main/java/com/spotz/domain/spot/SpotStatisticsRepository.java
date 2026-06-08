package com.spotz.domain.spot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SpotStatisticsRepository extends JpaRepository<SpotStatistics, Long> {

    Optional<SpotStatistics> findBySpotId(Long spotId);

    @Query("SELECT AVG(CAST(r.rating AS DOUBLE)) FROM Review r WHERE r.spot.spotId = :spotId")
    Double getAverageRating(@Param("spotId") Long spotId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.spot.spotId = :spotId")
    Long getReviewCount(@Param("spotId") Long spotId);

    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.spot.spotId = :spotId")
    Long getWishCount(@Param("spotId") Long spotId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.schedule.spot.spotId = :spotId AND t.status = 'RESERVED'")
    Long getReserveCount(@Param("spotId") Long spotId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE SPOT_STATISTICS s SET " +
            "s.average_rating = (SELECT AVG(r.rating) FROM REVIEW r WHERE r.spot_id = s.spot_id), " +
            "s.review_count = (SELECT COUNT(r.review_id) FROM REVIEW r WHERE r.spot_id = s.spot_id), " +
            "s.wish_count = (SELECT COUNT(w.wish_id) FROM WISHLIST w WHERE w.spot_id = s.spot_id), " +
            "s.reserve_count = (SELECT COUNT(t.ticket_id) FROM TICKET t JOIN SPOT_SCHEDULE ss ON t.schedule_id = ss.schedule_id WHERE ss.spot_id = s.spot_id AND t.status = 'RESERVED'), " +
            "s.updated_at = NOW() " +
            "WHERE s.spot_id = :spotId",
            nativeQuery = true)
    void syncStatistics(@Param("spotId") Long spotId);
}
