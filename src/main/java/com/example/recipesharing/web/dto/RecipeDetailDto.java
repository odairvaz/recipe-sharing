package com.example.recipesharing.web.dto;

import com.example.recipesharing.persistense.model.enums.RecipeCategory;

import java.time.LocalDateTime;
import java.util.List;

public record RecipeDetailDto(
        Long id,
        String title,
        String description,
        String ingredients,
        String instructions,
        RecipeCategory category,
        String imageUrl,
        LocalDateTime createdAt,
        String authorFirstName,
        String authorLastName,
        String authorEmail,
        String authorAvatarUrl,
        List<ReviewDto> reviews
) {}
