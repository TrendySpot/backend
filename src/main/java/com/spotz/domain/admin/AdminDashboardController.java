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
        Long revenue = paymentRepository.sumAmount();
        return ResponseEntity.ok(ApiResponse.of(DashboardResponse.builder()
                .totalMembers(memberRepository.count())
                .totalSpots(spotRepository.count())
                .totalTickets(ticketRepository.count())
                .totalReviews(reviewRepository.count())
                .totalRevenue(revenue != null ? revenue : 0L)
                .build()));
    }
}
