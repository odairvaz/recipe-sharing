package com.example.recipesharing.web.dto;

import com.example.recipesharing.persistense.model.enums.RecipeCategory;
import jakarta.validation.constraints.NotBlank;

public record RecipeCreateRequestDto(
        @NotBlank(message = "Title cannot be blank")
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
