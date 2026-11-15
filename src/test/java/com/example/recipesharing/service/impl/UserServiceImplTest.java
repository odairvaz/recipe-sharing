package com.example.recipesharing.service.impl;

import com.example.recipesharing.persistence.model.User;
import com.example.recipesharing.persistence.model.VerificationToken;
import com.example.recipesharing.persistence.repository.IUserRepository;
import com.example.recipesharing.persistence.repository.IVerificationTokenRepository;
import com.example.recipesharing.service.IFileStorageService;
import com.example.recipesharing.service.filestorage.StorageType;
import com.example.recipesharing.service.token.ActivationResult;
import com.example.recipesharing.web.dto.UserDto;
import com.example.recipesharing.web.error.UserAlreadyExistException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private IUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private IVerificationTokenRepository tokenRepository;
    @Mock
    private IFileStorageService fileStorageService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder, tokenRepository, fileStorageService);
    }

    @Test
    void registerNewUserAccount_storesAvatarAsAvatar() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile("avatarFile", "a.png", "image/png", new byte[]{1});
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        given(fileStorageService.storeFile(avatar, StorageType.AVATAR)).willReturn("http://localhost/avatars/a.png");
        given(passwordEncoder.encode("secret")).willReturn("hash");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.registerNewUserAccount(userDto("new@example.com"), avatar);

        assertThat(saved.getAvatarUrl()).isEqualTo("http://localhost/avatars/a.png");
        assertThat(saved.getPassword()).isEqualTo("hash");
        assertThat(saved.isEnabled()).isFalse();
    }

    @Test
    void registerNewUserAccount_existingEmail_throws() {
        given(userRepository.findByEmail("taken@example.com")).willReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.registerNewUserAccount(userDto("taken@example.com"), null))
                .isInstanceOf(UserAlreadyExistException.class);
    }

    @Test
    void activateUserByToken_validToken_enablesUserAndDeletesToken() {
        User user = new User();
        VerificationToken token = new VerificationToken("abc", user, LocalDateTime.now().plusHours(1));
        given(tokenRepository.findByToken("abc")).willReturn(Optional.of(token));

        assertThat(userService.activateUserByToken("abc")).isEqualTo(ActivationResult.SUCCESS);
        assertThat(user.isEnabled()).isTrue();
        then(tokenRepository).should().delete(token);
    }

    @Test
    void activateUserByToken_expiredToken() {
        User user = new User();
        given(tokenRepository.findByToken("old"))
                .willReturn(Optional.of(new VerificationToken("old", user, LocalDateTime.now().minusMinutes(1))));

        assertThat(userService.activateUserByToken("old")).isEqualTo(ActivationResult.TOKEN_EXPIRED);
        assertThat(user.isEnabled()).isFalse();
        then(userRepository).should(never()).save(any());
    }

    @Test
    void activateUserByToken_unknownToken() {
        given(tokenRepository.findByToken("nope")).willReturn(Optional.empty());

        assertThat(userService.activateUserByToken("nope")).isEqualTo(ActivationResult.TOKEN_INVALID);
    }

    private static UserDto userDto(String email) {
        UserDto dto = new UserDto();
        dto.setFirstName("New");
        dto.setLastName("User");
        dto.setEmail(email);
        dto.setPassword("secret");
        dto.setMatchingPassword("secret");
        return dto;
    }
}
