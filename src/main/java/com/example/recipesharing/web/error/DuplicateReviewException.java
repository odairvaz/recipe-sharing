package com.example.recipesharing.web.error;

public class DuplicateReviewException extends RuntimeException {

    public DuplicateReviewException(String message) {
        super(message);
    }
}
