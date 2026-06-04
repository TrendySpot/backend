package com.spotz.domain.spot;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpotScheduleRepository extends JpaRepository<SpotSchedule, Long> {
    List<SpotSchedule> findBySpotSpotIdOrderByEventDate(Long spotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SpotSchedule s WHERE s.scheduleId = :id")
    Optional<SpotSchedule> findByIdWithLock(@Param("id") Long id);
}
