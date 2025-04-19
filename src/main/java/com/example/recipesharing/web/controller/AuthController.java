package com.example.recipesharing.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String displayLoginPage() {
        return "login/form";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }

}
