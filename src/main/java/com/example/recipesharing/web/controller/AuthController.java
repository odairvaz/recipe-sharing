package com.example.recipesharing.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import static com.example.recipesharing.constants.ViewName.VIEW_LOGIN;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String displayLoginPage() {
        return VIEW_LOGIN;
    }

}
