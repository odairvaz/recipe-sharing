package com.example.recipesharing.web.controller;

import com.example.recipesharing.config.SecurityConfig;
import com.example.recipesharing.config.StorageProperties;
import com.example.recipesharing.persistence.model.User;
import com.example.recipesharing.registration.listener.OnRegistrationCompleteEvent;
import com.example.recipesharing.service.IUserService;
import com.example.recipesharing.service.impl.UserDetailsServiceImpl;
import com.example.recipesharing.service.token.ActivationResult;
import com.example.recipesharing.web.dto.UserDto;
import com.example.recipesharing.web.error.UserAlreadyExistException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.Locale;

import static com.example.recipesharing.constants.ViewName.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RegistrationController.class)
@Import({SecurityConfig.class, StorageProperties.class})
@TestPropertySource(properties = {
        "logDir=target/test-logs",
        "file.storage.base-location=target/test-uploads"
})
@RecordApplicationEvents
class RegistrationControllerTest {

    private static final String VALID_PASSWORD = "Str0ng#Pass";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationEvents events;

    @MockitoBean
    private IUserService userService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private MockMultipartFile avatarFile;
    private User registeredUser;

    @BeforeEach
    void setUp() {
        avatarFile = new MockMultipartFile("avatarFile", "avatar.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});
        registeredUser = new User();
        registeredUser.setEmail("test.user@example.com");
        registeredUser.setFirstName("Test");
    }

    @Test
    @DisplayName("GET /register shows the registration form")
    void displayRegistrationForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_REGISTRATION_FORM))
                .andExpect(model().attribute("registrationRequest", instanceOf(UserDto.class)));
    }

    @Test
    @DisplayName("POST /register with valid data registers the user and publishes the registration event")
    void registerUser_validData() throws Exception {
        given(userService.registerNewUserAccount(any(UserDto.class), any())).willReturn(registeredUser);

        mockMvc.perform(validRegistration())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_REGISTRATION_SUCCESS))
                .andExpect(model().attributeExists("registrationSuccessMsg"));

        assertThat(events.stream(OnRegistrationCompleteEvent.class))
                .singleElement()
                .satisfies(event -> assertThat(event.getUser()).isSameAs(registeredUser));
    }

    @Test
    @DisplayName("POST /register with an existing email shows a field error on email")
    void registerUser_existingEmail() throws Exception {
        given(userService.registerNewUserAccount(any(UserDto.class), any()))
                .willThrow(new UserAlreadyExistException("Email exists"));

        mockMvc.perform(validRegistration())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_REGISTRATION_FORM))
                .andExpect(model().attributeHasFieldErrorCode("registrationRequest", "email", "error.user"));

        assertThat(events.stream(OnRegistrationCompleteEvent.class)).isEmpty();
    }

    @Test
    @DisplayName("POST /register with a weak password shows the form again without registering")
    void registerUser_weakPassword() throws Exception {
        mockMvc.perform(registration("123", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_REGISTRATION_FORM))
                .andExpect(model().attributeHasFieldErrors("registrationRequest", "password"));

        then(userService).should(never()).registerNewUserAccount(any(), any());
    }

    @Test
    @DisplayName("POST /register with mismatching passwords shows the form again")
    void registerUser_passwordMismatch() throws Exception {
        mockMvc.perform(registration(VALID_PASSWORD, VALID_PASSWORD + "x"))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_REGISTRATION_FORM))
                .andExpect(model().attributeHasFieldErrors("registrationRequest", "password"));

        then(userService).should(never()).registerNewUserAccount(any(), any());
    }

    @Test
    @DisplayName("POST /register shows the error view on an unexpected failure")
    void registerUser_unexpectedFailure() throws Exception {
        given(userService.registerNewUserAccount(any(UserDto.class), any()))
                .willThrow(new RuntimeException("database down"));

        mockMvc.perform(validRegistration())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_REGISTRATION_ERROR))
                .andExpect(model().attributeExists("registrationErrorMsg"));
    }

    @Test
    @DisplayName("GET /register/confirm maps each activation result to its view")
    void confirmRegistration() throws Exception {
        assertConfirmView(ActivationResult.SUCCESS, VIEW_VERIFICATION_SUCCESS);
        assertConfirmView(ActivationResult.ALREADY_ACTIVE, VIEW_VERIFICATION_SUCCESS);
        assertConfirmView(ActivationResult.TOKEN_EXPIRED, VIEW_VERIFICATION_EXPIRED);
        assertConfirmView(ActivationResult.TOKEN_INVALID, VIEW_VERIFICATION_INVALID_TOKEN);
    }

    private void assertConfirmView(ActivationResult result, String expectedView) throws Exception {
        String token = "token-" + result;
        given(userService.activateUserByToken(token)).willReturn(result);

        mockMvc.perform(get("/register/confirm").param("token", token).locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name(expectedView));
    }

    private MockMultipartHttpServletRequestBuilder validRegistration() {
        return registration(VALID_PASSWORD, VALID_PASSWORD);
    }

    private MockMultipartHttpServletRequestBuilder registration(String password, String matchingPassword) {
        return (MockMultipartHttpServletRequestBuilder) multipart("/register")
                .file(avatarFile)
                .param("firstName", "Test")
                .param("lastName", "User")
                .param("email", "test.user@example.com")
                .param("password", password)
                .param("matchingPassword", matchingPassword)
                .locale(Locale.ENGLISH)
                .with(csrf());
    }
}
