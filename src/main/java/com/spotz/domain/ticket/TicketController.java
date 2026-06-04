package com.spotz.domain.ticket;

import com.spotz.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> reserve(
            @AuthenticationPrincipal Long memberId,
            @RequestBody ReserveRequest req) {
        return ResponseEntity.ok(ApiResponse.of(ticketService.reserve(memberId, req)));
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long ticketId) {
        ticketService.cancel(memberId, ticketId);
        return ResponseEntity.ok(ApiResponse.success("예약이 취소되었습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getMyTickets(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.of(ticketService.getMyTickets(memberId)));
    }
}
