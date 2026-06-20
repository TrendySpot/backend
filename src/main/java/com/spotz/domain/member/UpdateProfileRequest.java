package com.spotz.domain.member;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String nickname;
    private String profileImage;
    private String currentPassword; // ⭕ [추가] 검증을 위한 현재 비밀번호
    private String newPassword;     // ⭕ [변경] 바꾸고 싶은 새 비밀번호 (선택 사항)
}