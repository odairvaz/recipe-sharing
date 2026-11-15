package com.example.recipesharing.persistense.repository;

import com.example.recipesharing.persistense.model.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IRecipeRepository extends JpaRepository<Recipe, Long> {

    @Override
    @EntityGraph(attributePaths = "author")
    Page<Recipe> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "reviews", "reviews.user"})
    @Query("SELECT r FROM Recipe r WHERE r.id = :id")
    Optional<Recipe> findDetailById(@Param("id") Long id);

    @EntityGraph(attributePaths = "author")
    @Query(value = "SELECT r FROM Recipe r WHERE " +
            "LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(r.ingredients) LIKE LOWER(CONCAT('%', :keyword, '%'))",
            countQuery = "SELECT COUNT(r) FROM Recipe r WHERE " +
                    "LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    "LOWER(r.ingredients) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Recipe> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

}
