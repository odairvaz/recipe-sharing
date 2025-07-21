package com.example.recipesharing.persistense.repository;

import com.example.recipesharing.persistense.model.Favorite;
import com.example.recipesharing.persistense.model.Recipe;
import com.example.recipesharing.persistense.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface IFavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserAndRecipe(User user, Recipe recipe);

    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);

    Optional<Favorite> findByUserAndRecipe(User user, Recipe recipe);

    void deleteByUserAndRecipe(User user, Recipe recipe);

    void deleteByUserIdAndRecipeId(Long userId, Long recipeId);

    Page<Favorite> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    @Query("SELECT f.recipe.id FROM Favorite f WHERE f.user = :user AND f.recipe.id IN :recipeIds")
    Set<Long> findFavoritedRecipeIdsByUserInList(@Param("user") User user, @Param("recipeIds") List<Long> recipeIds);

}
