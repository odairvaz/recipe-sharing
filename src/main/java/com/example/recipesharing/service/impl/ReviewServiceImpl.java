package com.example.recipesharing.service.impl;

import com.example.recipesharing.persistence.model.Recipe;
import com.example.recipesharing.persistence.model.Review;
import com.example.recipesharing.persistence.model.User;
import com.example.recipesharing.persistence.repository.IRecipeRepository;
import com.example.recipesharing.persistence.repository.IReviewRepository;
import com.example.recipesharing.service.IReviewService;
import com.example.recipesharing.web.dto.ReviewSubmissionDto;
import com.example.recipesharing.web.error.DuplicateReviewException;
import com.example.recipesharing.web.error.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements IReviewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final IReviewRepository reviewRepository;
    private final IRecipeRepository recipeRepository;
    private final CacheManager cacheManager;


    public ReviewServiceImpl(IReviewRepository reviewRepository, IRecipeRepository recipeRepository, CacheManager cacheManager) {
        this.reviewRepository = reviewRepository;
        this.recipeRepository = recipeRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    public void addReview(Long recipeId, ReviewSubmissionDto reviewDto, User author) {

        LOGGER.debug("Attempting to add review by user '{}' for recipe ID {}", author.getEmail(), recipeId);

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", "id", recipeId));

        if (reviewRepository.existsByUserIdAndRecipeId(author.getId(), recipeId)) {
            throw new DuplicateReviewException("You have already reviewed this recipe.");
        }

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
    @Transactional
    public void deleteReview(Long reviewId, String currentUsername) throws AccessDeniedException {
        LOGGER.debug("Attempting to delete review ID {} by user '{}'", reviewId, currentUsername);

        Review review = reviewRepository.findByIdFetchingUser(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        if (!review.getUser().getEmail().equals(currentUsername)) {
            throw new AccessDeniedException("User '" + currentUsername + "' does not own review " + reviewId);
        }

        Long recipeId = review.getRecipe().getId();
        reviewRepository.delete(review);
        evictRecipeDetailCache(recipeId);
        LOGGER.info("Deleted review ID {} and evicted recipeDetail::{} from cache", reviewId, recipeId);
    }

    private void evictRecipeDetailCache(Long recipeId) {
        var cache = cacheManager.getCache("recipeDetail");
        if (cache != null) {
            cache.evict(recipeId);
        }
    }
}
