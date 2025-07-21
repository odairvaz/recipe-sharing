package com.example.recipesharing.service.impl;

import com.example.recipesharing.persistense.model.Favorite;
import com.example.recipesharing.persistense.model.Recipe;
import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.persistense.repository.IFavoriteRepository;
import com.example.recipesharing.persistense.repository.IRecipeRepository;
import com.example.recipesharing.persistense.repository.IUserRepository;
import com.example.recipesharing.service.IFavoriteService;
import com.example.recipesharing.service.IRecipeService;
import com.example.recipesharing.web.dto.RecipeSummaryDto;
import com.example.recipesharing.web.error.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IFavoriteServiceImpl implements IFavoriteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IFavoriteServiceImpl.class);

    private final IFavoriteRepository favoritesRepository;
    private final IRecipeRepository recipeRepository;

    public IFavoriteServiceImpl(IFavoriteRepository favoritesRepository,
                                IRecipeRepository recipeRepository) {
        this.favoritesRepository = favoritesRepository;
        this.recipeRepository = recipeRepository;
    }

    private Recipe getRecipeById(Long recipeId) {
        return recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", "id", recipeId));
    }

    @Override
    @Transactional
    public void addFavorite(Long recipeId, User user) {
        Recipe recipe = getRecipeById(recipeId);

        if (!favoritesRepository.existsByUserAndRecipe(user, recipe)) {
            Favorite favorite = new Favorite(recipe, user);
            favoritesRepository.save(favorite);
            LOGGER.debug("User '{}' added recipe ID {} in favorite", user, recipeId);
        } else {
            LOGGER.debug("User '{}' already had recipe ID {} in favourites", user, recipeId);
        }
    }

    @Override
    @Transactional
    public void removeFavorite(Long recipeId, User user) {
        favoritesRepository.deleteByUserIdAndRecipeId(user.getId(), recipeId);
        LOGGER.debug("User '{}' deleted recipe ID {} from favorite", user, recipeId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRecipeFavoritedByUser(Long recipeId, User user) {
        try {
            return favoritesRepository.existsByUserIdAndRecipeId(user.getId(), recipeId);
        } catch (ResourceNotFoundException e) {
            LOGGER.warn("Resource not found during: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeSummaryDto> findFavoritedRecipesByUser(User user, Pageable pageable) {
        Page<Favorite> favoritePage = favoritesRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        List<Long> recipeIdsOnPage = favoritePage.getContent().stream()
                .map(fav -> fav.getRecipe().getId())
                .collect(Collectors.toList());
        Set<Long> favoritedIds = Set.copyOf(recipeIdsOnPage);

        List<RecipeSummaryDto> dtoList = favoritePage.getContent().stream()
                .map(Favorite::getRecipe)
                .map(recipe -> mapToSummaryDtoWithFavorite(recipe, favoritedIds))
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, favoritePage.getTotalElements());
    }


    @Override
    @Transactional(readOnly = true)
    public Set<Long> findUserFavoriteRecipeIdsInList(User user, List<Long> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return Collections.emptySet();
        }
        try {
            return favoritesRepository.findFavoritedRecipeIdsByUserInList(user, recipeIds);
        } catch (ResourceNotFoundException e) {
            LOGGER.warn("User '{}' not found trying to find favorite IDs in list.", user);
            return Collections.emptySet();
        }
    }

    private RecipeSummaryDto mapToSummaryDtoWithFavorite(Recipe recipe, Set<Long> favoritedRecipeIds) {
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

}
