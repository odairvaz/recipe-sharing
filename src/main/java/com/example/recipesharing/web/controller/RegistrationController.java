package com.example.recipesharing.web.controller;

import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.persistense.model.VerificationToken;
import com.example.recipesharing.registration.listener.OnRegistrationCompleteEvent;
import com.example.recipesharing.service.IUserService;
import com.example.recipesharing.web.dto.UserDto;
import com.example.recipesharing.web.error.UserAlreadyExistException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/api")
public class RegistrationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationController.class);

    private final IUserService userService;
    private final ApplicationEventPublisher eventPublisher;


    public RegistrationController(IUserService userService, ApplicationEventPublisher eventPublisher) {
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/register")
    public String displayRegistrationForm(Model model) {
        model.addAttribute("registrationRequest", new UserDto());
        return "registration/form";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("registrationRequest") @Valid UserDto registrationRequest, BindingResult bindingResult, HttpServletRequest request, Model model) {

        if (bindingResult.hasErrors()) {
            return "registration/form";
        }

        try {
            User registeredUser = userService.registerNewUserAccount(registrationRequest);
            String applicationUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            eventPublisher.publishEvent(new OnRegistrationCompleteEvent(registeredUser, request.getLocale(), applicationUrl, null));
        } catch (UserAlreadyExistException uaeEx) {
            LOGGER.error("User email already exists: {}", registrationRequest.getEmail(), uaeEx);
            bindingResult.rejectValue("email", "error.user", "There is already a user registered with the email provided.");
            return "registration/form";
        } catch (RuntimeException ex) {
            LOGGER.error("Unable to register user ", ex);
            model.addAttribute("registrationErrorMsg", "An error occurred when sending the email!");
            return "registration/error";
        }
        model.addAttribute("registrationSuccessMsg", "You registered successfully. We will send you a confirmation message to your email account.");
        return "registration/success";
    }

    @GetMapping("/register/confirm")
    public String confirmRegistration(@RequestParam("token") String token, Model model) {
        Optional<VerificationToken> optionalToken = userService.getVerificationToken(token);
        if (optionalToken.isPresent()) {
            VerificationToken verificationToken = optionalToken.get();
            if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                model.addAttribute("registrationSuccessMsg", "Your registration token has expired.");
                return "registration/verification_expired";
            }
            User user = optionalToken.get().getUser();
            user.setEnabled(true);
            userService.saveRegisteredUser(user);
            model.addAttribute("registrationSuccessMsg", "Your account has been activated successfully. You can now log in");
            return "registration/verification_success";
        }
        return "redirect:/api/register/success";
    }

}
