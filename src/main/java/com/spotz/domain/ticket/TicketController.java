package com.spotz.domain.ticket;

import com.spotz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

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
