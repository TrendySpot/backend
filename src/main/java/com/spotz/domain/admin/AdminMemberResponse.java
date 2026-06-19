package com.spotz.domain.admin;

import com.spotz.domain.member.Member;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminMemberResponse {
    private Long memberId;
    private String email;
    private String nickname;
    private String profileImage;
    private String role;
    private String provider;
    private LocalDateTime createdAt;

    public static AdminMemberResponse from(Member m) {
        return AdminMemberResponse.builder()
                .memberId(m.getMemberId()).email(m.getEmail())
                .nickname(m.getNickname()).profileImage(m.getProfileImage())
                .role(m.getRole().name()).provider(m.getProvider())
                .createdAt(m.getCreatedAt()).build();
    }
}
