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

    // [작성, 06월 12일 10:48] 스팟 삭제 비즈니스 로직(SpotService)에서 연관된 전체 스케줄을 조회하기 위한 메서드 추가
    List<SpotSchedule> findBySpotSpotId(Long spotId);
}