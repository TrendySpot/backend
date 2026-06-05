package com.spotz.domain.payment;

import com.spotz.domain.ticket.Ticket;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "PAYMENT")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    // 포트원 V2 명칭에 맞게 필드명 변경 (기존 impUid 역할)
    @Column(name = "portone_payment_id", nullable = false, length = 100)
    private String portonePaymentId;

    @Column(name = "merchant_uid", nullable = false, length = 100)
    private String merchantUid;

    @Column(nullable = false)
    private Integer amount;

    // @Builder.Default 어노테이션을 붙여주어야 빌더 사용 시에도 "PAID"가 기본값으로 세팅됩니다.
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PAID";

    @CreationTimestamp
    @Column(name = "paid_at", updatable = false)
    private LocalDateTime paidAt;
}