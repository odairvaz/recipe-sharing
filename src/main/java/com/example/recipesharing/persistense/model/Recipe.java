package com.example.recipesharing.persistense.model;

import com.example.recipesharing.persistense.model.enums.RecipeCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"author", "reviews", "favorites"})
@ToString(exclude = {"author", "reviews", "favorites"})
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String ingredients;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Enumerated(EnumType.STRING)
    private RecipeCategory category;

    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites = new ArrayList<>();

    @Column(name = "created_at")
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt;

    public Recipe(String title, String description, String ingredients, String instructions, RecipeCategory category, String imageUrl, User author, List<Review> reviews, List<Favorite> favorites) {
        this.title = title;
        this.description = description;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.category = category;
        this.imageUrl = imageUrl;
        this.author = author;
        this.reviews = reviews;
        this.favorites = favorites;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void addReview(Review review) {
        reviews.add(review);
        review.setRecipe(this);
    }

    public void removeReview(Review review) {
        reviews.remove(review);
        review.setRecipe(null);
    }

}
