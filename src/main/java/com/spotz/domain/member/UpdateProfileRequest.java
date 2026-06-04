package com.spotz.domain.member;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String nickname;
    private String profileImage;
    private String password;
}
