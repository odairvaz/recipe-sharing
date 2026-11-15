package com.example.recipesharing.web;

import com.example.recipesharing.persistence.model.Recipe;
import com.example.recipesharing.persistence.model.User;
import com.example.recipesharing.persistence.model.enums.RecipeCategory;
import com.example.recipesharing.persistence.repository.IReviewRepository;
import com.example.recipesharing.persistence.repository.IUserRepository;
import com.example.recipesharing.service.IRecipeService;
import com.example.recipesharing.service.IReviewService;
import com.example.recipesharing.service.impl.UserDetailsServiceImpl;
import com.example.recipesharing.support.AbstractIntegrationTest;
import com.example.recipesharing.web.dto.RecipeCreateRequestDto;
import com.example.recipesharing.web.dto.RecipeSummaryDto;
import com.example.recipesharing.web.dto.ReviewSubmissionDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RecipeFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Str0ng#Pass";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IReviewRepository reviewRepository;
    @Autowired
    private IRecipeService recipeService;
    @Autowired
    private IReviewService reviewService;
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void unconfirmedUserCannotLogIn() throws Exception {
        User user = createUser(false);

        mockMvc.perform(formLogin("/login").user("email", user.getEmail()).password(PASSWORD))
                .andExpect(redirectedUrl("/login?disabled"));
    }

    @Test
    void confirmedUserCanLogIn() throws Exception {
        User user = createUser(true);

        mockMvc.perform(formLogin("/login").user("email", user.getEmail()).password(PASSWORD))
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void addingRecipeRedirectsHomeWithSuccessMessage() throws Exception {
        User author = createUser(true);

        mockMvc.perform(multipart("/recipe/add")
                        .param("title", "Pancakes")
                        .param("ingredients", "Flour, milk, eggs")
                        .param("instructions", "Mix and fry")
                        .param("category", "BREAKFAST")
                        .with(user(details(author)))
                        .with(csrf()))
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attribute("success", "Recipe 'Pancakes' added successfully!"));
    }

    @Test
    void userCannotDeleteSomeoneElsesReview() throws Exception {
        User author = createUser(true);
        User reviewer = createUser(true);
        Recipe recipe = createRecipe(author);
        reviewService.addReview(recipe.getId(), new ReviewSubmissionDto(4, "Nice"), reviewer);
        Long reviewId = reviewRepository.findAll().stream()
                .filter(r -> r.getRecipe().getId().equals(recipe.getId()))
                .findFirst().orElseThrow().getId();

        mockMvc.perform(post("/recipe/{recipeId}/reviews/{reviewId}", recipe.getId(), reviewId)
                        .with(user(details(author)))
                        .with(csrf()))
                .andExpect(redirectedUrl("/recipe/" + recipe.getId()))
                .andExpect(flash().attribute("reviewError", "Could not delete review: You are not the owner."));

        assertThat(reviewRepository.existsById(reviewId)).isTrue();

        mockMvc.perform(post("/recipe/{recipeId}/reviews/{reviewId}", recipe.getId(), reviewId)
                        .with(user(details(reviewer)))
                        .with(csrf()))
                .andExpect(flash().attribute("reviewSuccess", "Review deleted successfully."));

        assertThat(reviewRepository.existsById(reviewId)).isFalse();
    }

    @Test
    void secondReviewBySameUserIsRejectedWithMessage() throws Exception {
        User author = createUser(true);
        User reviewer = createUser(true);
        Recipe recipe = createRecipe(author);

        mockMvc.perform(post("/recipe/{recipeId}/reviews", recipe.getId())
                        .param("rating", "5")
                        .param("comment", "Great")
                        .with(user(details(reviewer)))
                        .with(csrf()))
                .andExpect(flash().attribute("reviewSuccess", "Your review has been submitted successfully!"));

        mockMvc.perform(post("/recipe/{recipeId}/reviews", recipe.getId())
                        .param("rating", "3")
                        .param("comment", "Again")
                        .with(user(details(reviewer)))
                        .with(csrf()))
                .andExpect(flash().attribute("reviewError", "You have already reviewed this recipe."));
    }

    @Test
    void favoritesAreShownPerUserEvenWhenListingIsCached() throws Exception {
        User author = createUser(true);
        User fan = createUser(true);
        Recipe recipe = createRecipe(author);

        mockMvc.perform(post("/recipe/{recipeId}/favorite/toggle", recipe.getId())
                        .header("Referer", "https://evil.example.com/search?keyword=soup")
                        .with(user(details(fan)))
                        .with(csrf()))
                // Only the path of the Referer is used, so a foreign host can never be a redirect target.
                .andExpect(redirectedUrl("/search?keyword=soup"));

        // The first request fills the shared cache; the second must still reflect the other user's favorites.
        assertThat(isFavoritedOnHome(fan, recipe)).isTrue();
        assertThat(isFavoritedOnHome(author, recipe)).isFalse();
        assertThat(isFavoritedOnHome(fan, recipe)).isTrue();
    }

    @Test
    void recipeDetailShowsDeleteButtonOnlyToReviewOwner() throws Exception {
        User author = createUser(true);
        User reviewer = createUser(true);
        Recipe recipe = createRecipe(author);
        reviewService.addReview(recipe.getId(), new ReviewSubmissionDto(5, "Lovely soup"), reviewer);

        String asReviewer = mockMvc.perform(get("/recipe/{id}", recipe.getId()).with(user(details(reviewer))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String asAuthor = mockMvc.perform(get("/recipe/{id}", recipe.getId()).with(user(details(author))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(asReviewer).contains("Lovely soup").contains("btn-outline-danger");
        assertThat(asAuthor).contains("Lovely soup").doesNotContain("btn-outline-danger");
    }

    @Test
    void unknownRecipeReturns404() throws Exception {
        User user = createUser(true);

        mockMvc.perform(get("/recipe/{id}", Long.MAX_VALUE).with(user(details(user))))
                .andExpect(status().isNotFound());
    }

    @SuppressWarnings("unchecked")
    private boolean isFavoritedOnHome(User user, Recipe recipe) throws Exception {
        MvcResult result = mockMvc.perform(get("/home").param("size", "50").with(user(details(user))))
                .andExpect(status().isOk())
                .andReturn();
        Page<RecipeSummaryDto> page = (Page<RecipeSummaryDto>) result.getModelAndView().getModel().get("recipePage");
        return page.getContent().stream()
                .filter(summary -> summary.id().equals(recipe.getId()))
                .findFirst().orElseThrow()
                .isFavorited();
    }

    private User createUser(boolean enabled) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    private Recipe createRecipe(User author) {
        return recipeService.addNewRecipe(
                new RecipeCreateRequestDto("Soup " + UUID.randomUUID(), "Warm", "Water", "Boil", RecipeCategory.SOUP),
                null, author);
    }

    private UserDetails details(User user) {
        return userDetailsService.loadUserByUsername(user.getEmail());
    }
}
