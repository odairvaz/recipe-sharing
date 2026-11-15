package com.example.recipesharing.web.controller;


import com.example.recipesharing.persistence.model.Recipe;
import com.example.recipesharing.persistence.model.User;
import com.example.recipesharing.persistence.model.enums.RecipeCategory;
import com.example.recipesharing.security.core.userdetails.RecipeUserDetails;
import com.example.recipesharing.service.IFavoriteService;
import com.example.recipesharing.service.IRecipeService;
import com.example.recipesharing.service.IReviewService;
import com.example.recipesharing.service.IUserService;
import com.example.recipesharing.web.dto.RecipeCreateRequestDto;
import com.example.recipesharing.web.dto.RecipeDetailDto;
import com.example.recipesharing.web.dto.RecipeSummaryDto;
import com.example.recipesharing.web.dto.ReviewSubmissionDto;
import com.example.recipesharing.web.error.DuplicateReviewException;
import com.example.recipesharing.web.error.InvalidFileException;
import com.example.recipesharing.web.error.ResourceNotFoundException;
import com.example.recipesharing.web.util.RedirectUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.example.recipesharing.constants.ViewName.*;

@Controller
public class RecipeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeController.class);

    private final IRecipeService recipeService;
    private final IUserService userService;
    private final IReviewService reviewService;
    private final IFavoriteService favoriteService;

    public RecipeController(IRecipeService recipeService, IUserService userService, IReviewService reviewService, IFavoriteService favoriteService) {
        this.recipeService = recipeService;
        this.userService = userService;
        this.reviewService = reviewService;
        this.favoriteService = favoriteService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model,
                       @PageableDefault(size = 9, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                       @AuthenticationPrincipal RecipeUserDetails currentUserDetails) {

        Page<RecipeSummaryDto> recipeDtoPage = recipeService.findAllSummaries(pageable, currentUser(currentUserDetails));
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
                            @RequestParam(value = "recipeImage", required = false) MultipartFile recipeImage,
                            @AuthenticationPrincipal RecipeUserDetails currentUserDetails,
                            RedirectAttributes redirectAttributes,
                            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("allCategories", RecipeCategory.values());
            return VIEW_RECIPE_FORM;
        }

        try {
            Recipe savedRecipe = recipeService.addNewRecipe(recipeRequest, recipeImage, currentUser(currentUserDetails));
            redirectAttributes.addFlashAttribute("success", "Recipe '" + savedRecipe.getTitle() + "' added successfully!");
            LOGGER.info("Successfully added recipe ID: {}", savedRecipe.getId());
            return "redirect:/home";
        } catch (InvalidFileException e) {
            model.addAttribute("allCategories", RecipeCategory.values());
            model.addAttribute("error", e.getMessage());
            return VIEW_RECIPE_FORM;
        }
    }

    @GetMapping("/recipe/{recipeId}")
    public String recipeDetail(Model model, @PathVariable Long recipeId) {
        model.addAttribute("recipe", findRecipeDetail(recipeId));
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

        if (bindingResult.hasErrors()) {
            LOGGER.debug("Validation errors submitting review for recipe ID {}: {}", recipeId, bindingResult.getAllErrors());
            model.addAttribute("recipe", findRecipeDetail(recipeId));
            return VIEW_RECIPE_DETAIL;
        }

        try {
            reviewService.addReview(recipeId, reviewDto, currentUser(currentUserDetails));
            redirectAttributes.addFlashAttribute("reviewSuccess", "Your review has been submitted successfully!");
        } catch (DuplicateReviewException e) {
            redirectAttributes.addFlashAttribute("reviewError", e.getMessage());
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
        } catch (AccessDeniedException e) {
            LOGGER.warn("Unauthorized attempt by user {} to delete review ID {}", currentUserDetails.getEmail(), reviewId);
            redirectAttributes.addFlashAttribute("reviewError", "Could not delete review: You are not the owner.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("reviewError", "That review no longer exists.");
        }

        return "redirect:/recipe/" + recipeId;
    }

    @PostMapping("/recipe/{recipeId}/favorite/toggle")
    public String toggleFavorite(@PathVariable Long recipeId,
                                 @AuthenticationPrincipal RecipeUserDetails currentUserDetails,
                                 RedirectAttributes redirectAttributes,
                                 @RequestHeader(value = HttpHeaders.REFERER, required = false) final String referer) {

        User user = currentUser(currentUserDetails);
        try {
            if (favoriteService.isRecipeFavoritedByUser(recipeId, user)) {
                favoriteService.removeFavorite(recipeId, user);
                redirectAttributes.addFlashAttribute("favoriteSuccess", "Recipe removed from favorites.");
            } else {
                favoriteService.addFavorite(recipeId, user);
                redirectAttributes.addFlashAttribute("favoriteSuccess", "Recipe added to favorites!");
            }
        } catch (ResourceNotFoundException e) {
            LOGGER.warn("Toggle favorite failed: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("favoriteError", "Could not change favorite status: Resource not found.");
        }

        return "redirect:" + RedirectUtils.safeLocalPath(referer, "/recipe/" + recipeId);
    }

    @GetMapping("/my-favorites")
    public String myFavorites(Model model,
                              @AuthenticationPrincipal RecipeUserDetails currentUserDetails,
                              @PageableDefault(size = 9, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<RecipeSummaryDto> favoriteRecipePage = favoriteService.findFavoritedRecipesByUser(currentUser(currentUserDetails), pageable);
        model.addAttribute("favoriteRecipePage", favoriteRecipePage);
        return VIEW_USER_FAVORITES;
    }

    @GetMapping("/search")
    public String searchRecipes(@RequestParam(value = "keyword", required = false) String keyword,
                                Model model,
                                @AuthenticationPrincipal RecipeUserDetails currentUserDetails,
                                @PageableDefault(size = 9, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<RecipeSummaryDto> recipePage = Page.empty(pageable);
        if (keyword != null && !keyword.isBlank()) {
            LOGGER.debug("Performing search for keyword: {}", keyword);
            recipePage = recipeService.searchRecipes(keyword, pageable, currentUser(currentUserDetails));
            model.addAttribute("keyword", keyword);
        }

        model.addAttribute("recipePage", recipePage);
        return VIEW_SEARCH_RESULTS;
    }

    private User currentUser(RecipeUserDetails currentUserDetails) {
        return userService.findUserById(currentUserDetails.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user no longer exists: " + currentUserDetails.getEmail()));
    }

    private RecipeDetailDto findRecipeDetail(Long recipeId) {
        return recipeService.findRecipeDetailById(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe", "id", recipeId));
    }

}
