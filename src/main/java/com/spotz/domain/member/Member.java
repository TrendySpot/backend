package com.spotz.domain.member;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "MEMBER")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(name = "profile_image", length = 500)
    @Builder.Default
    private String profileImage = "https://your-s3-bucket.s3.amazonaws.com/default-profile.png";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Column(length = 20)
    @Builder.Default
    private String provider = "LOCAL";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // [작성 06월 10일 16:39] 비밀번호 재설정을 위해 엔티티 내부 password 필드를 안전하게 변경하는 메서드 추가
    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public enum Role {
        ROLE_USER, ROLE_ADMIN
    }
}