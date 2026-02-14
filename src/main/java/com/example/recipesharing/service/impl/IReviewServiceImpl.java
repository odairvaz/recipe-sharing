package com.example.recipesharing.service.impl;

import com.example.recipesharing.persistense.model.Recipe;
import com.example.recipesharing.persistense.model.Review;
import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.persistense.repository.IRecipeRepository;
import com.example.recipesharing.persistense.repository.IReviewRepository;
import com.example.recipesharing.service.IReviewService;
import com.example.recipesharing.web.dto.ReviewSubmissionDto;
import com.example.recipesharing.web.error.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IReviewServiceImpl implements IReviewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IReviewServiceImpl.class);

    private final IReviewRepository reviewRepository;
    private final IRecipeRepository recipeRepository;
    private final CacheManager cacheManager;


    public IReviewServiceImpl(IReviewRepository reviewRepository, IRecipeRepository recipeRepository, CacheManager cacheManager) {
        this.reviewRepository = reviewRepository;
        this.recipeRepository = recipeRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    public void addReview(Long recipeId, ReviewSubmissionDto reviewDto, User author) {

        LOGGER.warn("Attempting to add review by user '{}' for recipe ID {}", author.getEmail(), recipeId);

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> {
                    LOGGER.debug("Recipe not found with ID: {}", recipeId);
                     return new EntityNotFoundException("Recipe not found with id " + recipeId);
                });

        Review newReview = new Review();
        newReview.setRating(reviewDto.rating());
        newReview.setComment(reviewDto.comment());
        newReview.setRecipe(recipe);
        newReview.setUser(author);

        Review savedReview = reviewRepository.save(newReview);
        evictRecipeDetailCache(recipeId);
        LOGGER.info("Successfully added review with ID {} for recipe ID {} by user '{}'",
                savedReview.getId(),
                recipeId,
                author.getEmail());

    }

    @Override
    public void deleteReview(Long reviewId, String currentUsername) throws AccessDeniedException {
        LOGGER.warn("Attempting to delete review ID {} by user '{}'", reviewId, currentUsername);

        Review review = reviewRepository.findByIdFetchingUser(reviewId)
                .orElseThrow(() -> {
                    LOGGER.warn("Review not found with ID: {}", reviewId);
                    return new ResourceNotFoundException("Review", "id", reviewId);
                });

        Long recipeId = review.getRecipe().getId();
        reviewRepository.delete(review);
        evictRecipeDetailCache(recipeId);
        LOGGER.info("Deleted review ID {} — evicted recipeDetail::{} from cache", reviewId, recipeId);
    }

    private void evictRecipeDetailCache(Long recipeId) {
        var cache = cacheManager.getCache("recipeDetail");
        if (cache != null) {
            cache.evict(recipeId);
        }
    }
}
