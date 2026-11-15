package com.example.recipesharing.web.dto;

import com.example.recipesharing.persistense.model.enums.RecipeCategory;

import java.io.Serializable;
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
        LocalDateTime createdAt,
        boolean isFavorited
) implements Serializable {

    public RecipeSummaryDto withFavorited(boolean favorited) {
        return new RecipeSummaryDto(id, title, description, imageUrl, category, authorFirstName, authorLastName,
                authorEmail, createdAt, favorited);
    }
}
