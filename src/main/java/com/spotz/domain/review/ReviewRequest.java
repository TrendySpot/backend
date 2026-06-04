package com.spotz.domain.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotBlank @Size(max = 1000)
    private String content;
}
