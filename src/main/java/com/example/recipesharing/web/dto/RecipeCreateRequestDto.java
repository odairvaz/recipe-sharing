package com.example.recipesharing.web.dto;

import com.example.recipesharing.persistence.model.enums.RecipeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecipeCreateRequestDto(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 255, message = "Title is too long (max 255 characters)")
        String title,

        String description,

        @NotBlank(message = "Ingredients cannot be blank")
        String ingredients,

        @NotBlank(message = "Instructions cannot be blank")
        String instructions,

        RecipeCategory category
) {
    public static RecipeCreateRequestDto empty() {
        return new RecipeCreateRequestDto("", "", "", "", null);
    }
}
