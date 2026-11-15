package com.example.recipesharing.service.impl;

import com.example.recipesharing.persistense.model.Favorite;
import com.example.recipesharing.persistense.model.Recipe;
import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.persistense.repository.IFavoriteRepository;
import com.example.recipesharing.persistense.repository.IRecipeRepository;
import com.example.recipesharing.service.IFavoriteService;
import com.example.recipesharing.service.mapper.RecipeMapper;
import com.example.recipesharing.web.dto.RecipeSummaryDto;
import com.example.recipesharing.web.error.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class IFavoriteServiceImpl implements IFavoriteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IFavoriteServiceImpl.class);

    private final IFavoriteRepository favoritesRepository;
    private final IRecipeRepository recipeRepository;
    private final RecipeMapper recipeMapper;

    public IFavoriteServiceImpl(IFavoriteRepository favoritesRepository,
                                IRecipeRepository recipeRepository,
                                RecipeMapper recipeMapper) {
        this.favoritesRepository = favoritesRepository;
        this.recipeRepository = recipeRepository;
        this.recipeMapper = recipeMapper;
    }

    @Override
    @Transactional
    public void addFavorite(Long recipeId, User user) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", "id", recipeId));

        if (!favoritesRepository.existsByUserAndRecipe(user, recipe)) {
            favoritesRepository.save(new Favorite(recipe, user));
            LOGGER.debug("User '{}' added recipe ID {} to favorites", user.getEmail(), recipeId);
        } else {
            LOGGER.debug("User '{}' already had recipe ID {} in favorites", user.getEmail(), recipeId);
        }
    }

    @Override
    @Transactional
    public void removeFavorite(Long recipeId, User user) {
        favoritesRepository.deleteByUserIdAndRecipeId(user.getId(), recipeId);
        LOGGER.debug("User '{}' removed recipe ID {} from favorites", user.getEmail(), recipeId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRecipeFavoritedByUser(Long recipeId, User user) {
        return favoritesRepository.existsByUserIdAndRecipeId(user.getId(), recipeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryDto> findFavoritedRecipesByUser(User user, Pageable pageable) {
        return favoritesRepository.findByUser(user, pageable)
                .map(favorite -> recipeMapper.toSummaryDto(favorite.getRecipe(), true));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findUserFavoriteRecipeIdsInList(User user, List<Long> recipeIds) {
        if (user == null || recipeIds == null || recipeIds.isEmpty()) {
            return Collections.emptySet();
        }
        return favoritesRepository.findFavoritedRecipeIdsByUserInList(user, recipeIds);
    }

}
