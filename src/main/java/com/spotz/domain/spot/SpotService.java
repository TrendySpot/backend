package com.spotz.domain.spot;

// [작성, 06월 12일 11:41] 완전 삭제(Hard Delete) 연쇄 처리에 필요한 연관 레포지토리 및 엔티티 임포트 유지
import com.spotz.domain.ticket.Ticket;
import com.spotz.domain.ticket.TicketRepository;
import com.spotz.domain.payment.PaymentRepository;
import com.spotz.domain.review.ReviewRepository;
import com.spotz.domain.wishlist.WishlistRepository;
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

    private final TicketRepository ticketRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;

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

    // [수정, 06월 12일 11:41] 비활성화(논리 삭제) 방식을 폐기하고, 외래키 제약조건 위배를 방지하며 자식 데이터까지 완전히 물리 삭제하는 로직으로 원복
    @Transactional
    public void deleteSpot(Long spotId) {
        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스팟입니다."));

        // 1 스팟 하위의 모든 스케줄을 순회하며 연관된 결제 및 티켓 데이터 선삭제
        List<SpotSchedule> schedules = scheduleRepository.findBySpotSpotId(spotId);
        for (SpotSchedule schedule : schedules) {
            List<Ticket> tickets = ticketRepository.findByScheduleScheduleId(schedule.getScheduleId());
            for (Ticket ticket : tickets) {
                // 결제 데이터 선삭제
                paymentRepository.deleteByTicketTicketId(ticket.getTicketId());
            }
            // 티켓 데이터 일괄 삭제
            ticketRepository.deleteAll(tickets);
        }

        // ️2 스케줄 데이터 일괄 삭제
        scheduleRepository.deleteAll(schedules);

        // 33스팟에 얽혀 있는 리뷰 및 찜 내역 일괄 삭제
        reviewRepository.deleteBySpotSpotId(spotId);
        wishlistRepository.deleteBySpotSpotId(spotId);

        // 4최상위 부모인 스팟 데이터를 데이터베이스에서 최종적으로 완전히 삭제
        spotRepository.delete(spot);
    }
}