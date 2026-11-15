package com.example.recipesharing.service.impl;

import com.example.recipesharing.persistense.model.Recipe;
import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.persistense.repository.IRecipeRepository;
import com.example.recipesharing.service.IFavoriteService;
import com.example.recipesharing.service.IFileStorageService;
import com.example.recipesharing.service.IRecipeService;
import com.example.recipesharing.service.filestorage.StorageType;
import com.example.recipesharing.service.mapper.RecipeMapper;
import com.example.recipesharing.web.dto.RecipeCreateRequestDto;
import com.example.recipesharing.web.dto.RecipeDetailDto;
import com.example.recipesharing.web.dto.RecipeSummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Set;

@Service
public class IRecipeServiceImpl implements IRecipeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IRecipeServiceImpl.class);

    private final IRecipeRepository recipeRepository;
    private final IFileStorageService fileStorageService;
    private final IFavoriteService favoriteService;
    private final RecipeSummaryCache recipeSummaryCache;
    private final RecipeMapper recipeMapper;

    public IRecipeServiceImpl(IRecipeRepository recipeRepository, IFileStorageService fileStorageService,
                              IFavoriteService favoriteService, RecipeSummaryCache recipeSummaryCache,
                              RecipeMapper recipeMapper) {
        this.recipeRepository = recipeRepository;
        this.fileStorageService = fileStorageService;
        this.favoriteService = favoriteService;
        this.recipeSummaryCache = recipeSummaryCache;
        this.recipeMapper = recipeMapper;
    }

    @Override
    @Cacheable(value = "recipeDetail", key = "#recipeId")
    @Transactional(readOnly = true)
    public Optional<RecipeDetailDto> findRecipeDetailById(Long recipeId) {
        return recipeRepository.findDetailById(recipeId).map(recipeMapper::toDetailDto);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = RecipeSummaryCache.RECIPE_SUMMARIES, allEntries = true),
            @CacheEvict(value = RecipeSummaryCache.SEARCH_RESULTS, allEntries = true)
    })
    @Transactional
    public Recipe addNewRecipe(RecipeCreateRequestDto recipeDto, MultipartFile recipeImage, User author) {
        String imageUrl = null;
        if (recipeImage != null && !recipeImage.isEmpty()) {
            try {
                imageUrl = fileStorageService.storeFile(recipeImage, StorageType.RECIPE_IMAGE);
                LOGGER.info("Stored image file {} for recipe {}", imageUrl, recipeDto.title());
            } catch (IOException e) {
                LOGGER.error("Could not store image for recipe '{}': {}", recipeDto.title(), e.getMessage());
                throw new UncheckedIOException("Failed to save recipe image.", e);
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
    public Page<RecipeSummaryDto> findAllSummaries(Pageable pageable, User user) {
        return applyFavorites(recipeSummaryCache.findAll(pageable), user);
    }

    @Override
    public Page<RecipeSummaryDto> searchRecipes(String keyword, Pageable pageable, User user) {
        if (keyword == null || keyword.isBlank()) {
            return Page.empty(pageable);
        }
        return applyFavorites(recipeSummaryCache.search(keyword, pageable), user);
    }

    private Page<RecipeSummaryDto> applyFavorites(Page<RecipeSummaryDto> page, User user) {
        if (user == null || page.isEmpty()) {
            return page;
        }
        Set<Long> favoritedIds = favoriteService.findUserFavoriteRecipeIdsInList(
                user, page.getContent().stream().map(RecipeSummaryDto::id).toList());
        return page.map(summary -> favoritedIds.contains(summary.id()) ? summary.withFavorited(true) : summary);
    }

}
