package com.example.recipesharing.web.dto;

import java.time.LocalDateTime;

public record ReviewDto(
        int rating,
        String comment,
        String reviewerFirstName,
        String reviewerLastName,
        String reviewerEmail,
        LocalDateTime reviewDate
) {}
