package com.example.recipesharing.service.impl;

import com.example.recipesharing.persistence.model.Recipe;
import com.example.recipesharing.persistence.model.Review;
import com.example.recipesharing.persistence.model.User;
import com.example.recipesharing.persistence.repository.IRecipeRepository;
import com.example.recipesharing.persistence.repository.IReviewRepository;
import com.example.recipesharing.web.dto.ReviewSubmissionDto;
import com.example.recipesharing.web.error.DuplicateReviewException;
import com.example.recipesharing.web.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private IReviewRepository reviewRepository;
    @Mock
    private IRecipeRepository recipeRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache recipeDetailCache;

    private ReviewServiceImpl reviewService;
    private User owner;
    private Recipe recipe;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(reviewRepository, recipeRepository, cacheManager);
        owner = user(1L, "owner@example.com");
        recipe = new Recipe();
        ReflectionTestUtils.setField(recipe, "id", 10L);
    }

    @Test
    void deleteReview_byOwner_deletesAndEvictsCache() {
        Review review = new Review(recipe, owner, 5, "Great");
        given(reviewRepository.findByIdFetchingUser(100L)).willReturn(Optional.of(review));
        given(cacheManager.getCache("recipeDetail")).willReturn(recipeDetailCache);

        reviewService.deleteReview(100L, "owner@example.com");

        then(reviewRepository).should().delete(review);
        then(recipeDetailCache).should().evict(10L);
    }

    @Test
    void deleteReview_bySomeoneElse_isDenied() {
        given(reviewRepository.findByIdFetchingUser(100L)).willReturn(Optional.of(new Review(recipe, owner, 5, "Great")));

        assertThatThrownBy(() -> reviewService.deleteReview(100L, "intruder@example.com"))
                .isInstanceOf(AccessDeniedException.class);

        then(reviewRepository).should(never()).delete(any());
    }

    @Test
    void deleteReview_missing_throwsNotFound() {
        given(reviewRepository.findByIdFetchingUser(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(100L, "owner@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addReview_twice_isRejected() {
        given(recipeRepository.findById(10L)).willReturn(Optional.of(recipe));
        given(reviewRepository.existsByUserIdAndRecipeId(1L, 10L)).willReturn(true);

        assertThatThrownBy(() -> reviewService.addReview(10L, new ReviewSubmissionDto(4, "Again"), owner))
                .isInstanceOf(DuplicateReviewException.class);

        then(reviewRepository).should(never()).save(any());
    }

    @Test
    void addReview_unknownRecipe_throwsNotFound() {
        given(recipeRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.addReview(10L, new ReviewSubmissionDto(4, "Hi"), owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static User user(Long id, String email) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setEmail(email);
        return user;
    }
}
