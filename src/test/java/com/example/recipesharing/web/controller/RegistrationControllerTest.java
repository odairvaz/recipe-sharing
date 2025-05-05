package com.example.recipesharing.web.controller;

import com.example.recipesharing.config.SecurityConfig;
import com.example.recipesharing.constants.ViewName;
import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.registration.listener.OnRegistrationCompleteEvent;
import com.example.recipesharing.registration.listener.RegistrationListener;
import com.example.recipesharing.service.ActivationResult;
import com.example.recipesharing.service.IFileStorageService;
import com.example.recipesharing.service.IUserService;
import com.example.recipesharing.web.dto.UserDto;
import com.example.recipesharing.web.error.InvalidFileException;
import com.example.recipesharing.web.error.UserAlreadyExistException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static com.example.recipesharing.constants.ViewName.*;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistrationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = { "logDir=./build/test-logs" })
class RegistrationControllerTest {
    /*
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUserService userService;

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private MessageSource messageSource;

    @MockitoBean
    private RegistrationListener registrationListener;

    @MockitoBean
    private IFileStorageService fileStorageService;

    private UserDto validUserDto;
    private MockMultipartFile mockAvatarFile;
    private User registeredUser;

    @BeforeEach
    void setUp() {
        validUserDto = new UserDto();
        validUserDto.setFirstName("Test");
        validUserDto.setLastName("User");
        validUserDto.setEmail("test.user@example.com");
        validUserDto.setPassword("Password1#3!");
        validUserDto.setMatchingPassword("Password1#3!");
        validUserDto.setBio("Test bio");

        mockAvatarFile = new MockMultipartFile(
                "avatarFile",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                "test-image-content".getBytes()
        );

        registeredUser = new User();
        registeredUser.setEmail(validUserDto.getEmail());
        registeredUser.setFirstName(validUserDto.getFirstName());

        given(messageSource.getMessage(eq("message.reg.success"), isNull(), any(Locale.class)))
                .willReturn("Registration successful message.");
        given(messageSource.getMessage(eq("message.reg.error.generic"), isNull(), any(Locale.class)))
                .willReturn("Generic registration error message.");
        given(messageSource.getMessage(eq("message.confirm.success"), isNull(), any(Locale.class)))
                .willReturn("Activation successful message.");
        given(messageSource.getMessage(eq("message.confirm.error.expired"), isNull(), any(Locale.class)))
                .willReturn("Token expired message.");
        given(messageSource.getMessage(eq("message.confirm.error.invalidToken"), isNull(), any(Locale.class)))
                .willReturn("Invalid token message.");
    }

    @Test
    @DisplayName("GET /register should return registration form view")
    void displayRegistrationForm_ShouldReturnFormView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("registration/form"))
                .andExpect(model().attributeExists("registrationRequest"))
                .andExpect(model().attribute("registrationRequest", instanceOf(UserDto.class)));
    }

    @Test
    @DisplayName("POST /register with valid data and avatar should register user and trigger listener")
    void registerUser_WithValidDataAndAvatar_ShouldRegisterAndTriggerListener() throws Exception {
        String mockAvatarUrl = "/avatars/mock-uuid.png";
        String expectedSuccessMessage = "Registration successful message.";

        given(fileStorageService.storeAvatar(any(MockMultipartFile.class), anyString()))
                .willReturn(mockAvatarUrl);
        given(userService.registerNewUserAccount(any(UserDto.class), eq(mockAvatarUrl)))
                .willReturn(registeredUser);

        mockMvc.perform(multipart("/register")
                        .file(mockAvatarFile)
                        .flashAttr("registrationRequest", validUserDto)
                        .locale(Locale.ENGLISH)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_REGISTRATION_SUCCESS))
                .andExpect(model().attribute("registrationSuccessMsg", expectedSuccessMessage))
                .andExpect(model().hasNoErrors());

        then(fileStorageService).should().storeAvatar(any(MockMultipartFile.class), anyString());
        then(userService).should().registerNewUserAccount(any(UserDto.class), eq(mockAvatarUrl));

        ArgumentCaptor<OnRegistrationCompleteEvent> eventCaptor =
                ArgumentCaptor.forClass(OnRegistrationCompleteEvent.class);

        then(registrationListener).should(times(1)).onApplicationEvent(eventCaptor.capture());

        OnRegistrationCompleteEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent, "Captured event should not be null");
        assertEquals(registeredUser, capturedEvent.getUser(), "User in event should match registered user");
    }

    @Test
    @DisplayName("POST /register with valid data and NO avatar should register user and return success view")
    void registerUser_WithValidDataNoAvatar_ShouldRegisterAndReturnSuccess() throws Exception {
        MockMultipartFile emptyAvatarFile = new MockMultipartFile("avatarFile", "", "application/octet-stream", new byte[0]);
        String expectedSuccessMessage = "Registration successful message.";

        given(userService.registerNewUserAccount(any(UserDto.class), isNull()))
                .willReturn(registeredUser);

        mockMvc.perform(multipart("/register")
                        .file(emptyAvatarFile)
                        .flashAttr("registrationRequest", validUserDto)
                        .locale(Locale.ENGLISH)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_REGISTRATION_SUCCESS))
                .andExpect(model().attribute("registrationSuccessMsg", expectedSuccessMessage))
                .andExpect(model().hasNoErrors());

        then(fileStorageService).should(never()).storeAvatar(any(), anyString());
        then(userService).should().registerNewUserAccount(any(UserDto.class), isNull());

        ArgumentCaptor<OnRegistrationCompleteEvent> eventCaptor =
                ArgumentCaptor.forClass(OnRegistrationCompleteEvent.class);

        then(registrationListener).should(times(1)).onApplicationEvent(eventCaptor.capture());

        OnRegistrationCompleteEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent, "Captured event should not be null");
        assertEquals(registeredUser, capturedEvent.getUser(), "User in event should match registered user");
    }

    @Test
    @DisplayName("POST /register with existing email should return form view with specific email error")
    void registerUser_WithExistingEmail_ShouldReturnFormWithEmailError() throws Exception {
        given(userService.registerNewUserAccount(any(UserDto.class), isNull()))
                .willThrow(new UserAlreadyExistException("Email exists"));

        MockMultipartFile emptyAvatarFile = new MockMultipartFile("avatarFile", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/register")
                        .file(emptyAvatarFile)
                        .flashAttr("registrationRequest", validUserDto)
                        .locale(Locale.ENGLISH)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_REGISTRATION_FORM))
                .andExpect(model().attributeExists("registrationRequest"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrorCode("registrationRequest", "email", "error.user"));

        then(fileStorageService).shouldHaveNoInteractions();
        then(userService).should().registerNewUserAccount(any(UserDto.class), isNull());
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /register with avatar upload failure should return form view with upload error")
    void registerUser_WithAvatarUploadFailure_ShouldReturnFormWithUploadError() throws Exception {
        String uploadErrorMessage = "Invalid file type!";
        given(fileStorageService.storeAvatar(any(MockMultipartFile.class), anyString()))
                .willThrow(new InvalidFileException(uploadErrorMessage));

        mockMvc.perform(multipart("/register")
                        .file(mockAvatarFile)
                        .flashAttr("registrationRequest", validUserDto)
                        .locale(Locale.ENGLISH)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_REGISTRATION_FORM))
                .andExpect(model().attributeExists("registrationRequest"))
                .andExpect(model().hasErrors());

        then(fileStorageService).should().storeAvatar(any(MockMultipartFile.class), anyString());
        then(userService).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /register with generic runtime exception during registration should return error view")
    void registerUser_WithGenericRuntimeException_ShouldReturnErrorView() throws Exception {
        String expectedErrorMessage = "Generic registration error message.";
        String mockAvatarUrl = "/avatars/mock-uuid.png";
        given(fileStorageService.storeAvatar(any(MockMultipartFile.class), anyString())).willReturn(mockAvatarUrl);
        given(userService.registerNewUserAccount(any(UserDto.class), eq(mockAvatarUrl)))
                .willThrow(new RuntimeException("Unexpected database error"));

        mockMvc.perform(multipart("/register")
                        .file(mockAvatarFile)
                        .flashAttr("registrationRequest", validUserDto)
                        .locale(Locale.ENGLISH)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_REGISTRATION_ERROR))
                .andExpect(model().attribute("registrationErrorMsg", expectedErrorMessage))
                .andExpect(model().hasNoErrors());

        then(fileStorageService).should().storeAvatar(any(MockMultipartFile.class), anyString());
        then(userService).should().registerNewUserAccount(any(UserDto.class), eq(mockAvatarUrl));
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("POST /register with invalid data should return form view")
    void registerUser_WithInvalidData_ShouldRegisterAndReturnError() throws Exception {
        validUserDto.setPassword("123");
        String mockAvatarUrl = "/avatars/mock-uuid.png";

        given(fileStorageService.storeAvatar(any(MockMultipartFile.class), anyString()))
                .willReturn(mockAvatarUrl);
        given(userService.registerNewUserAccount(any(UserDto.class), eq(mockAvatarUrl)))
                .willReturn(registeredUser);

        mockMvc.perform(multipart("/register")
                        .file(mockAvatarFile)
                        .flashAttr("registrationRequest", validUserDto)
                        .locale(Locale.ENGLISH)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_REGISTRATION_FORM))
                .andExpect(model().hasErrors());

        then(fileStorageService).should(never()).storeAvatar(any(), anyString());
        then(userService).should(never()).registerNewUserAccount(any(UserDto.class), eq(mockAvatarUrl));

        ArgumentCaptor<OnRegistrationCompleteEvent> eventCaptor =
                ArgumentCaptor.forClass(OnRegistrationCompleteEvent.class);
        then(registrationListener).should(times(0)).onApplicationEvent(eventCaptor.capture());
    }

    @Test
    @DisplayName("GET /confirm with valid token should activate user and return success view")
    void confirmRegistration_WithValidToken_ShouldReturnViewSuccess() throws Exception {
        String token = "valid-token-123";
        String expectedSuccessMessage = "Activation successful message.";

        given(userService.activateUserByToken(token))
                .willReturn(ActivationResult.SUCCESS);

        mockMvc.perform(get("/register/confirm")
                .param("token", token)
                        .locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_VERIFICATION_SUCCESS))
        .andExpect(model().attribute("registrationSuccessMsg", expectedSuccessMessage))
        .andExpect(model().hasNoErrors());

        then(userService).should().activateUserByToken(eq(token));
    }

    @Test
    @DisplayName("GET /confirm with already active token should activate user and return success view")
    void confirmRegistration_WithAlreadyActiveToken_ShouldReturnViewSuccess() throws Exception {
        String token = "already-active-token-123";
        String expectedSuccessMessage = "Activation successful message.";

        given(userService.activateUserByToken(token))
                .willReturn(ActivationResult.ALREADY_ACTIVE);

        mockMvc.perform(get("/register/confirm")
                        .param("token", token)
                        .locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_VERIFICATION_SUCCESS))
                .andExpect(model().attribute("registrationSuccessMsg", expectedSuccessMessage))
                .andExpect(model().hasNoErrors());

        then(userService).should().activateUserByToken(eq(token));
    }

    @Test
    @DisplayName("GET /confirm with expired token should return expired view")
    void confirmRegistration_WithExpiredToken_ShouldReturnViewExpired() throws Exception {
        String token = "expired-token-123";
        String expectedExpiredMessage = "Token expired message.";

        given(userService.activateUserByToken(token))
                .willReturn(ActivationResult.TOKEN_EXPIRED);

        mockMvc.perform(get("/register/confirm")
                        .param("token", token)
                        .locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_VERIFICATION_EXPIRED))
                .andExpect(model().attribute("registrationErrorMsg", expectedExpiredMessage))
                .andExpect(model().hasNoErrors());

        then(userService).should().activateUserByToken(eq(token));
    }

    @Test
    @DisplayName("GET /confirm with invalid token should return invalid view")
    void confirmRegistration_WithInvalidToken_ShouldReturnViewInvalid() throws Exception {
        String token = "invalid-token-123";
        String expectedInvalidMessage = "Invalid token message.";

        given(userService.activateUserByToken(token))
                .willReturn(ActivationResult.TOKEN_INVALID);

        mockMvc.perform(get("/register/confirm")
                        .param("token", token)
                        .locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_VERIFICATION_INVALID_TOKEN))
                .andExpect(model().attribute("registrationErrorMsg", expectedInvalidMessage))
                .andExpect(model().hasNoErrors());

        then(userService).should().activateUserByToken(eq(token));
    }
*/
}