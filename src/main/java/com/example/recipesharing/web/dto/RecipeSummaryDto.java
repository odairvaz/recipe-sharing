package com.example.recipesharing.web.dto;

import java.time.LocalDateTime;

public record RecipeSummaryDto(
        Long id,
        String title,
        String description,
        String imageUrl,
        String category,
        String authorFirstName,
        String authorLastName,
        String authorEmail,
        LocalDateTime createdAt
) {
}
