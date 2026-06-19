package com.spotz.domain.admin;

import com.spotz.domain.member.MemberRepository;
import com.spotz.domain.payment.PaymentRepository;
import com.spotz.domain.review.ReviewRepository;
import com.spotz.domain.spot.SpotRepository;
import com.spotz.domain.ticket.TicketRepository;
import com.spotz.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final MemberRepository memberRepository;
    private final SpotRepository spotRepository;
    private final TicketRepository ticketRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        // sumAmount()가 Optional<Long>을 반환하므로 .orElse(0L)로 바로 기본값 처리를 해줍니다.
        Long revenue = paymentRepository.sumAmount().orElse(0L);

        return ResponseEntity.ok(ApiResponse.of(DashboardResponse.builder()
                .totalMembers(memberRepository.count())
                .totalSpots(spotRepository.count())
                .totalTickets(ticketRepository.count())
                .totalReviews(reviewRepository.count())
                .totalRevenue(revenue) // null 걱정 없이 안전하게 대입
                .build()));
    }
}