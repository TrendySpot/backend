package com.spotz.domain.spot;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpotService {

    private final SpotRepository spotRepository;
    private final SpotScheduleRepository scheduleRepository;

    public Page<SpotSummaryDto> searchSpots(SpotSearchCondition cond, Pageable pageable) {
        return spotRepository.search(cond, pageable)
                .map(spot -> {
                    List<SpotSchedule> schedules =
                            scheduleRepository.findBySpotSpotIdOrderByEventDate(spot.getSpotId());

                    return SpotSummaryDto.from(spot, schedules);
                });
    }

    public SpotDetailDto getSpotDetail(Long spotId) {
        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스팟입니다."));
        List<SpotSchedule> schedules = scheduleRepository.findBySpotSpotIdOrderByEventDate(spotId);
        return SpotDetailDto.from(spot, schedules);
    }
}
