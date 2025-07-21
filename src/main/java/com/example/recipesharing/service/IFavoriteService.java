package com.example.recipesharing.service;

import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.web.dto.RecipeSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface IFavoriteService {

    void addFavorite(Long recipeId, User user);

    void removeFavorite(Long recipeId, User user);

    boolean isRecipeFavoritedByUser(Long recipeId, User user);

    Page<RecipeSummaryDto> findFavoritedRecipesByUser(User user, Pageable pageable);

    Set<Long> findUserFavoriteRecipeIdsInList(User user, List<Long> recipeIds);
}
