package com.example.recipesharing.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RecipeDetailDto(
        Long id,
        String title,
        String description,
        String ingredients,
        String instructions,
        String category,
        String imageUrl,
        LocalDateTime createdAt,
        String authorFirstName,
        String authorLastName,
        String authorEmail,
        String authorAvatarUrl,
        List<ReviewDto> reviews
) {}
