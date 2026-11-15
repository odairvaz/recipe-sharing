package com.example.recipesharing.service.mapper;

import com.example.recipesharing.persistense.model.Recipe;
import com.example.recipesharing.persistense.model.Review;
import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.web.dto.RecipeDetailDto;
import com.example.recipesharing.web.dto.RecipeSummaryDto;
import com.example.recipesharing.web.dto.ReviewDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class RecipeMapper {

    public RecipeSummaryDto toSummaryDto(Recipe recipe, boolean isFavorited) {
        User author = recipe.getAuthor();
        return new RecipeSummaryDto(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getImageUrl(),
                recipe.getCategory(),
                author.getFirstName(),
                author.getLastName(),
                author.getEmail(),
                recipe.getCreatedAt(),
                isFavorited
        );
    }

    public RecipeDetailDto toDetailDto(Recipe recipe) {
        List<ReviewDto> reviewDtos = (recipe.getReviews() == null) ? Collections.emptyList() :
                recipe.getReviews().stream()
                        .map(this::toReviewDto)
                        .toList();

        User recipeAuthor = recipe.getAuthor();
        return new RecipeDetailDto(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getDescription(),
                recipe.getIngredients(),
                recipe.getInstructions(),
                recipe.getCategory(),
                recipe.getImageUrl(),
                recipe.getCreatedAt(),
                recipeAuthor.getFirstName(),
                recipeAuthor.getLastName(),
                recipeAuthor.getEmail(),
                recipeAuthor.getAvatarUrl(),
                reviewDtos
        );
    }

    public ReviewDto toReviewDto(Review review) {
        User reviewAuthor = review.getUser();
        return new ReviewDto(
                review.getId(),
                review.getRating(),
                review.getComment(),
                reviewAuthor.getFirstName(),
                reviewAuthor.getLastName(),
                reviewAuthor.getEmail(),
                review.getCreatedAt()
        );
    }
}
