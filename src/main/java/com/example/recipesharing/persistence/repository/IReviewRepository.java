package com.example.recipesharing.persistence.repository;

import com.example.recipesharing.persistence.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.id = :id")
    Optional<Review> findByIdFetchingUser(@Param("id") Long id);

    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);

}
