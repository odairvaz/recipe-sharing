package com.example.recipesharing.persistense.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorites", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "recipe_id"})})
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name ="created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Favorite() {}

    public Favorite(Recipe recipe, User user) {
        this.recipe = recipe;
        this.user = user;
    }

}

