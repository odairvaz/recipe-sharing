package com.example.recipesharing.service.impl;

import com.example.recipesharing.persistense.model.Recipe;
import com.example.recipesharing.persistense.model.Review;
import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.persistense.repository.IRecipeRepository;
import com.example.recipesharing.persistense.repository.IReviewRepository;
import com.example.recipesharing.service.IReviewService;
import com.example.recipesharing.web.dto.ReviewSubmissionDto;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IReviewServiceImpl implements IReviewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IReviewServiceImpl.class);

    private final IReviewRepository reviewRepository;
    private final IRecipeRepository recipeRepository;


    public IReviewServiceImpl(IReviewRepository reviewRepository, IRecipeRepository recipeRepository) {
        this.reviewRepository = reviewRepository;
        this.recipeRepository = recipeRepository;
    }

    @Override
    @Transactional
    public void addReview(Long recipeId, ReviewSubmissionDto reviewDto, User author) {

        LOGGER.debug("Attempting to add review by user '{}' for recipe ID {}", author.getEmail(), recipeId);

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
        LOGGER.info("Successfully added review with ID {} for recipe ID {} by user '{}'",
                savedReview.getId(),
                recipeId,
                author.getEmail());

    }
}
