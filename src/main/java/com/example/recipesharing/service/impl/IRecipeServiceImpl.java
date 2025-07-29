package com.example.recipesharing.service.impl;

import com.example.recipesharing.persistense.model.Recipe;
import com.example.recipesharing.persistense.model.Review;
import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.persistense.repository.IRecipeRepository;
import com.example.recipesharing.service.IFavoriteService;
import com.example.recipesharing.service.IFileStorageService;
import com.example.recipesharing.service.IRecipeService;
import com.example.recipesharing.service.filestorage.StorageType;
import com.example.recipesharing.web.dto.RecipeCreateRequestDto;
import com.example.recipesharing.web.dto.RecipeDetailDto;
import com.example.recipesharing.web.dto.RecipeSummaryDto;
import com.example.recipesharing.web.dto.ReviewDto;
import com.example.recipesharing.web.error.InvalidFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IRecipeServiceImpl implements IRecipeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IRecipeServiceImpl.class);

    private final IRecipeRepository recipeRepository;
    private final IFileStorageService fileStorageService;
    private final IFavoriteService favoriteService;


    public IRecipeServiceImpl(IRecipeRepository recipeRepository, IFileStorageService fileStorageService, IFavoriteService favoriteService) {
        this.recipeRepository = recipeRepository;
        this.fileStorageService = fileStorageService;
        this.favoriteService = favoriteService;
    }

    @Override
    public Optional<RecipeDetailDto> findRecipeDetailById(Long recipeId) {
        Optional<Recipe> recipeOpt = recipeRepository.findById(recipeId);
        return recipeOpt.map(this::mapToDetailDto);
    }

    @Override
    @Transactional
    public Recipe addNewRecipe(RecipeCreateRequestDto recipeDto, MultipartFile recipeImage, User author) {
        String imageUrl = null;
        if (recipeImage != null && !recipeImage.isEmpty()) {
            try {
                imageUrl = fileStorageService.storeFile(recipeImage, StorageType.RECIPE_IMAGE);
                LOGGER.info("Stored image file {} for recipe {}", imageUrl, recipeDto.title());
            } catch (IOException | InvalidFileException e) {
                LOGGER.error("Could not store image for recipe '{}': {}", recipeDto.title(), e.getMessage());
                throw new RuntimeException("Failed to save recipe image.", e);
            }
        }

        Recipe recipe = new Recipe();
        recipe.setTitle(recipeDto.title());
        recipe.setDescription(recipeDto.description());
        recipe.setIngredients(recipeDto.ingredients());
        recipe.setInstructions(recipeDto.instructions());
        recipe.setCategory(recipeDto.category());
        recipe.setImageUrl(imageUrl);
        recipe.setAuthor(author);
        Recipe savedRecipe = recipeRepository.save(recipe);
        LOGGER.info("Saved new recipe with ID {} and title '{}' by user {}", savedRecipe.getId(), savedRecipe.getTitle(), author.getEmail());
        return savedRecipe;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryDto> findAllSummaries(Pageable pageable, User user) {
        Page<Recipe> recipePage = recipeRepository.findAll(pageable);
        List<Long> recipeIdsOnPage = recipePage.getContent().stream().map(Recipe::getId).collect(Collectors.toList());
        Set<Long> favoritedIds = favoriteService.findUserFavoriteRecipeIdsInList(user, recipeIdsOnPage);

        List<RecipeSummaryDto> dtoList = recipePage.getContent()
                .stream()
                .map(recipe -> mapToSummaryDto(recipe, favoritedIds))
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, recipePage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryDto> searchRecipes(String keyword, Pageable pageable, User user) {
        if (keyword == null || keyword.isBlank()) {
            return Page.empty(pageable);
        }

        Page<Recipe> recipePage = recipeRepository.searchByKeyword(keyword, pageable);

        Set<Long> favoritedIds = Collections.emptySet();
        if (user != null) {
            List<Long> recipeIdsOnPage = recipePage.getContent().stream().map(Recipe::getId).collect(Collectors.toList());
            favoritedIds = favoriteService.findUserFavoriteRecipeIdsInList(user, recipeIdsOnPage);
        }

        Set<Long> finalFavoritedIds = favoritedIds;
        List<RecipeSummaryDto> dtoList = recipePage.getContent()
                .stream()
                .map(recipe -> mapToSummaryDto(recipe, finalFavoritedIds))
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, recipePage.getTotalElements());
    }

    private RecipeSummaryDto mapToSummaryDto(Recipe recipe, Set<Long> favoritedRecipeIds) {
        User author = recipe.getAuthor();
        boolean isFavorited = favoritedRecipeIds.contains(recipe.getId());

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

    private RecipeDetailDto mapToDetailDto(Recipe recipe) {
        List<ReviewDto> reviewDtos = (recipe.getReviews() == null) ? Collections.emptyList() :
                recipe.getReviews().stream()
                        .map(this::mapToReviewDto)
                        .collect(Collectors.toList());

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

    private ReviewDto mapToReviewDto(Review review) {
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
