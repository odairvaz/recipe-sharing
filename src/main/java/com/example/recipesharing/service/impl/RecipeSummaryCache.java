package com.example.recipesharing.service.impl;

import com.example.recipesharing.persistense.model.Recipe;
import com.example.recipesharing.persistense.repository.IRecipeRepository;
import com.example.recipesharing.service.mapper.RecipeMapper;
import com.example.recipesharing.web.dto.RecipeSummaryDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cached, user-independent recipe listings. Every summary is returned with {@code isFavorited = false};
 * {@link IRecipeServiceImpl} applies the current user's favorites on top, so one cache entry can be
 * shared between all users.
 */
@Component
public class RecipeSummaryCache {

    public static final String RECIPE_SUMMARIES = "recipeSummaries";
    public static final String SEARCH_RESULTS = "searchResults";

    private final IRecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;

    public RecipeSummaryCache(IRecipeRepository recipeRepository, RecipeMapper recipeMapper) {
        this.recipeRepository = recipeRepository;
        this.recipeMapper = recipeMapper;
    }

    @Cacheable(value = RECIPE_SUMMARIES,
            key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    @Transactional(readOnly = true)
    public Page<RecipeSummaryDto> findAll(Pageable pageable) {
        return toSummaryPage(recipeRepository.findAll(pageable), pageable);
    }

    @Cacheable(value = SEARCH_RESULTS,
            key = "#keyword.trim().toLowerCase() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    @Transactional(readOnly = true)
    public Page<RecipeSummaryDto> search(String keyword, Pageable pageable) {
        return toSummaryPage(recipeRepository.searchByKeyword(keyword.trim(), pageable), pageable);
    }

    private Page<RecipeSummaryDto> toSummaryPage(Page<Recipe> recipePage, Pageable pageable) {
        return new PageImpl<>(
                recipePage.getContent().stream().map(recipe -> recipeMapper.toSummaryDto(recipe, false)).toList(),
                pageable,
                recipePage.getTotalElements());
    }
}
