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

    @Column(name = "portone_payment_id", nullable = false, length = 100)
    private String portonePaymentId;

    @Column(name = "merchant_uid", nullable = false, length = 100)
    private String merchantUid;

    @Column(nullable = false)
    private Long amount;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PAID";

    @CreationTimestamp
    @Column(name = "paid_at", updatable = false)
    private LocalDateTime paidAt;
}
