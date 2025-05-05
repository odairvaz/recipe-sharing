package com.example.recipesharing.persistense.repository;

import com.example.recipesharing.persistense.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.id = :id")
    Optional<Review> findByIdFetchingUser(@Param("id") Long id);

}
