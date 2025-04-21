package com.example.recipesharing.web.controller;


import com.example.recipesharing.persistense.model.Recipe;
import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.security.core.userdetails.RecipeUserDetails;
import com.example.recipesharing.service.IRecipeService;
import com.example.recipesharing.service.IUserService;
import com.example.recipesharing.web.dto.RecipeCreateRequestDto;
import com.example.recipesharing.web.dto.RecipeDetailDto;
import com.example.recipesharing.web.dto.RecipeSummaryDto;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

    public RecipeController(IRecipeService recipeService, IUserService userService) {
        this.recipeService = recipeService;
        this.userService = userService;
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
        return VIEW_RECIPE_DETAIL;
    }

}
