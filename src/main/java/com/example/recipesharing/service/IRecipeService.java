package com.example.recipesharing.service;

import com.example.recipesharing.persistense.model.Recipe;
import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.web.dto.RecipeCreateRequestDto;
import com.example.recipesharing.web.dto.RecipeDetailDto;
import com.example.recipesharing.web.dto.RecipeSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface IRecipeService {

    Optional<RecipeDetailDto> findRecipeDetailById(Long recipeId);

    Recipe addNewRecipe(RecipeCreateRequestDto recipeDto, MultipartFile recipeImage, User author);

    Page<RecipeSummaryDto> findAllSummaries(Pageable pageable, User user);

}
