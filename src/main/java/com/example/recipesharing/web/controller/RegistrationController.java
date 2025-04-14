package com.example.recipesharing.web.controller;

import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.registration.listener.OnRegistrationCompleteEvent;
import com.example.recipesharing.service.ActivationResult;
import com.example.recipesharing.service.IUserService;
import com.example.recipesharing.service.impl.FileStorageService;
import com.example.recipesharing.web.dto.UserDto;
import com.example.recipesharing.web.error.UserAlreadyExistException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Locale;

@Controller
@RequestMapping("/register")
public class RegistrationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationController.class);

    private static final String VIEW_REGISTRATION_FORM = "registration/form";
    private static final String VIEW_REGISTRATION_SUCCESS = "registration/success";
    private static final String VIEW_REGISTRATION_ERROR = "registration/error";
    private static final String VIEW_VERIFICATION_EXPIRED = "registration/verification_expired";
    private static final String VIEW_VERIFICATION_SUCCESS = "registration/verification_success";
    private static final String VIEW_VERIFICATION_INVALID_TOKEN = "registration/verification_invalid_token";

    private final IUserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final MessageSource messageSource;
    private final FileStorageService fileStorageService;

    public RegistrationController(IUserService userService, ApplicationEventPublisher eventPublisher, MessageSource messageSource, FileStorageService fileStorageService) {
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.messageSource = messageSource;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String displayRegistrationForm(Model model) {
        model.addAttribute("registrationRequest", new UserDto());
        return VIEW_REGISTRATION_FORM;
    }

    @PostMapping
    public String registerUser(@ModelAttribute("registrationRequest") @Valid UserDto registrationRequest, BindingResult bindingResult, @RequestParam("avatarFile") MultipartFile avatarFile, Locale locale, Model model) {

        if (bindingResult.hasErrors()) {
            return VIEW_REGISTRATION_FORM;
        }

        try {
            User registeredUser = userService.registerNewUserAccount(registrationRequest);
            if (avatarFile != null && !avatarFile.isEmpty()) {
                String avatarUrl = fileStorageService.storeFile(avatarFile);
                registeredUser.setAvatarUrl(avatarUrl);
                userService.saveRegisteredUser(registeredUser);
            }
            String applicationUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            eventPublisher.publishEvent(new OnRegistrationCompleteEvent(registeredUser, applicationUrl, null));
        } catch (UserAlreadyExistException uaeEx) {
            LOGGER.error("User email already exists: {}", registrationRequest.getEmail(), uaeEx);
            bindingResult.rejectValue("email", "error.user");
            return VIEW_REGISTRATION_FORM;
        } catch (RuntimeException ex) {
            LOGGER.error("Unable to register user ", ex);
            String errorMsg = messageSource.getMessage("message.reg.error.generic", null, locale);
            model.addAttribute("registrationErrorMsg", errorMsg);
            return VIEW_REGISTRATION_ERROR;
        }
        String successMsg = messageSource.getMessage("message.reg.success", null, locale);
        model.addAttribute("registrationSuccessMsg", successMsg);
        return VIEW_REGISTRATION_SUCCESS;
    }

    @GetMapping("/confirm")
    public String confirmRegistration(@RequestParam("token") String token, Model model, Locale locale) {
        ActivationResult result = userService.activateUserByToken(token);
        switch (result) {
            case ALREADY_ACTIVE:
            case SUCCESS:
                String successMsg = messageSource.getMessage("message.confirm.success", null, locale);
                model.addAttribute("registrationSuccessMsg", successMsg);
                return VIEW_VERIFICATION_SUCCESS;

            case TOKEN_EXPIRED:
                String expiredMsg = messageSource.getMessage("message.confirm.error.expired", null, locale);
                model.addAttribute("registrationErrorMsg", expiredMsg);
                return VIEW_VERIFICATION_EXPIRED;

            case TOKEN_INVALID:
            default:
                String invalidMsg = messageSource.getMessage("message.confirm.error.invalidToken", null, locale);
                model.addAttribute("registrationErrorMsg", invalidMsg);
                return VIEW_VERIFICATION_INVALID_TOKEN;
        }
    }

}
