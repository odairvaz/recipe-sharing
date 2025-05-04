package com.example.recipesharing.web.controller;


import com.example.recipesharing.persistense.model.Recipe;
import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.persistense.model.enums.RecipeCategory;
import com.example.recipesharing.security.core.userdetails.RecipeUserDetails;
import com.example.recipesharing.service.IRecipeService;
import com.example.recipesharing.service.IReviewService;
import com.example.recipesharing.service.IUserService;
import com.example.recipesharing.web.dto.RecipeCreateRequestDto;
import com.example.recipesharing.web.dto.RecipeDetailDto;
import com.example.recipesharing.web.dto.RecipeSummaryDto;
import com.example.recipesharing.web.dto.ReviewSubmissionDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.example.recipesharing.constants.ViewName.*;

@Controller
public class RecipeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeController.class);

    private final IRecipeService recipeService;
    private final IUserService userService;
    private final IReviewService reviewService;

    public RecipeController(IRecipeService recipeService, IUserService userService, IReviewService reviewService) {
        this.recipeService = recipeService;
        this.userService = userService;
        this.reviewService = reviewService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model,
                       @PageableDefault(size = 2,
                       sort = "createdAt",
                       direction = Sort.Direction.DESC)
                       Pageable pageable) {

        Page<RecipeSummaryDto> recipeDtoPage = recipeService.findAllSummaries(pageable);
        model.addAttribute("recipePage", recipeDtoPage);
        return VIEW_HOME;
    }

    @GetMapping("/recipe/add")
    public String displayRecipeForm(Model model) {
        model.addAttribute("recipeRequest", RecipeCreateRequestDto.empty());
        model.addAttribute("allCategories", RecipeCategory.values());
        return VIEW_RECIPE_FORM;
    }

    @PostMapping("/recipe/add")
    public String addRecipe(@ModelAttribute("recipeRequest") @Valid RecipeCreateRequestDto recipeRequest,
                            BindingResult bindingResult,
                            @RequestParam("recipeImage") MultipartFile recipeImage,
                            @AuthenticationPrincipal RecipeUserDetails currentUserDetails,
                            RedirectAttributes redirectAttributes,
                            Model model) {

        if (bindingResult.hasErrors()) {
            return VIEW_RECIPE_FORM;
        }

        try {
            User author = userService.findUserByEmail(currentUserDetails.getEmail())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
            Recipe savedRecipe = recipeService.addNewRecipe(recipeRequest, recipeImage, author);
            redirectAttributes.addFlashAttribute("success", "Ripe '" + savedRecipe.getTitle() + "' added successfully!");
            LOGGER.info("Successfully added recipe ID: {}", savedRecipe.getId());
            return VIEW_HOME;
        } catch (Exception e) {
            LOGGER.error("Error adding recipe: {}", e.getMessage(), e);
            model.addAttribute("recipeRequest", recipeRequest);
            model.addAttribute("error", "An error occurred while saving the recipe. Please try again. Error: " + e.getMessage());
            return VIEW_RECIPE_FORM;
        }
    }

    @GetMapping("/recipe/{recipeId}")
    public String recipeDetail(Model model, @PathVariable Long recipeId) {
        LOGGER.debug("Requesting recipe detail page for ID: {}", recipeId);

        RecipeDetailDto recipeDto = recipeService.findRecipeDetailById(recipeId)
                .orElseThrow(() -> {
                    LOGGER.warn("Recipe not found for ID: {}", recipeId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
                });

        model.addAttribute("recipe", recipeDto);

        if (!model.containsAttribute("newReview")) {
            model.addAttribute("newReview", ReviewSubmissionDto.empty());
        }
        return VIEW_RECIPE_DETAIL;
    }

    @PostMapping("/recipe/{recipeId}/reviews")
    public String addReview(@PathVariable Long recipeId,
                            @Valid @ModelAttribute("newReview") ReviewSubmissionDto reviewDto,
                            BindingResult bindingResult,
                            @AuthenticationPrincipal RecipeUserDetails currentUserDetails,
                            RedirectAttributes redirectAttributes,
                            Model model) {

        LOGGER.debug("Received review submission for recipe ID {}: {}", recipeId, reviewDto);

        if (currentUserDetails == null) {
            LOGGER.warn("Unauthenticated user attempted to submit review for recipe ID {}", recipeId);
            redirectAttributes.addFlashAttribute("error", "You must be logged in to submit a review.");
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            LOGGER.warn("Validation errors submitting review for recipe ID {}: {}", recipeId, bindingResult.getAllErrors());
            RecipeDetailDto recipeDto = recipeService.findRecipeDetailById(recipeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
            model.addAttribute("recipe", recipeDto);
            return VIEW_RECIPE_DETAIL;
        }

        try {
            User author = userService.findUserByEmail(currentUserDetails.getEmail())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found:"));

            reviewService.addReview(recipeId, reviewDto, author);
            redirectAttributes.addFlashAttribute("reviewSuccess", "Your review has been submitted successfully!");
            LOGGER.info("Review added successfully for recipe ID {} by user {}", recipeId, author.getEmail());

        } catch (Exception e) {
            LOGGER.error("Unexpected error adding review for recipe ID {}: {}", recipeId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("reviewError", "An unexpected error occurred while submitting your review.");
        }
        return "redirect:/recipe/" + recipeId;
    }

    @PostMapping("/recipe/{recipeId}/reviews/{reviewId}")
    public String deleteReview(@PathVariable Long recipeId,
                               @PathVariable Long reviewId,
                               @AuthenticationPrincipal RecipeUserDetails currentUserDetails,
                               RedirectAttributes redirectAttributes) {

        try {
            reviewService.deleteReview(reviewId, currentUserDetails.getEmail());
            redirectAttributes.addFlashAttribute("reviewSuccess", "Review deleted successfully.");
            LOGGER.debug("Review ID {} deleted successfully by user {}", reviewId, currentUserDetails.getEmail());

        } catch (AccessDeniedException e) {
            LOGGER.warn("Unauthorized attempt by user {} to delete review ID {}", currentUserDetails.getEmail(), reviewId);
            redirectAttributes.addFlashAttribute("reviewError", "Could not delete review: You are not the owner.");
        } catch (Exception e) {
            LOGGER.error("Error deleting review ID {}: {}", reviewId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("reviewError", "An error occurred while deleting the review.");
        }

        return "redirect:/recipe/" + recipeId;
    }

}
