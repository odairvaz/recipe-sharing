package com.example.recipesharing.persistense.repository;

import com.example.recipesharing.persistense.model.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(@NotBlank String email);
}
