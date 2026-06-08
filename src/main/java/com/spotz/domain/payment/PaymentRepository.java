package com.spotz.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 1. 반환 타입을 Optional<Long>으로 변경하여 Null 예외 방지
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'PAID'")
    Optional<Long> sumAmount();
}