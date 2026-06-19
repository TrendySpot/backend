package com.spotz.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 1. 반환 타입을 Optional<Long>으로 변경하여 Null 예외 방지
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'PAID'")
    Optional<Long> sumAmount();

    // [작성, 06월 12일 10:16] 외래키 제약조건 위배를 해결하기 위해 티켓 ID로 결제 데이터를 삭제하는 메서드 추가
    void deleteByTicketTicketId(Long ticketId);
}