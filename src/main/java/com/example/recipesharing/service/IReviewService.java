package com.example.recipesharing.service;

import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.web.dto.ReviewSubmissionDto;

public interface IReviewService {

    void addReview(Long recipeId, ReviewSubmissionDto reviewDto, User author);
}
