package com.example.recipesharing.service;

import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.web.dto.ReviewSubmissionDto;
import org.springframework.security.access.AccessDeniedException;

public interface IReviewService {

    void addReview(Long recipeId, ReviewSubmissionDto reviewDto, User author);

    void deleteReview(Long reviewId, String currentUsername) throws AccessDeniedException;

}
