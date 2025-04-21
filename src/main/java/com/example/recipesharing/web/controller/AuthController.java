package com.example.recipesharing.web.controller;

import com.example.recipesharing.persistense.model.Recipe;
import com.example.recipesharing.service.IRecipeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    private final IRecipeService recipeService;

    public AuthController(IRecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/login")
    public String displayLoginPage() {
        return "login/form";
    }

}
