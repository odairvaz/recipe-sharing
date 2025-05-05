package com.example.recipesharing.web.dto;

import jakarta.validation.constraints.*;

public record ReviewSubmissionDto(
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        Integer rating,

        @NotBlank(message = "Comment cannot be empty")
        @Size(max = 1000, message = "Comment is too long (max 1000 characters)")
        String comment
) {
    public static ReviewSubmissionDto empty() {
        return new ReviewSubmissionDto(null, "");
    }
}
