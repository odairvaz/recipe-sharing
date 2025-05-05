package com.example.recipesharing.web.dto;

import com.example.recipesharing.persistense.model.enums.RecipeCategory;

import java.time.LocalDateTime;

public record RecipeSummaryDto(
        Long id,
        String title,
        String description,
        String imageUrl,
        RecipeCategory category,
        String authorFirstName,
        String authorLastName,
        String authorEmail,
        LocalDateTime createdAt
) {
}
