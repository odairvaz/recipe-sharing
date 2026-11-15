package com.example.recipesharing.service.impl;

import com.example.recipesharing.persistence.model.User;
import com.example.recipesharing.persistence.repository.IUserRepository;
import com.example.recipesharing.security.core.userdetails.RecipeUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private IUserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void loadUserByUsername_reflectsAccountActivation(boolean enabled) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setFirstName("Ann");
        user.setEmail("ann@example.com");
        user.setPassword("hash");
        user.setEnabled(enabled);
        given(userRepository.findByEmail("ann@example.com")).willReturn(Optional.of(user));

        RecipeUserDetails details = (RecipeUserDetails) userDetailsService.loadUserByUsername("ann@example.com");

        assertThat(details.isEnabled()).isEqualTo(enabled);
        assertThat(details.getId()).isEqualTo(7L);
        assertThat(details.getEmail()).isEqualTo("ann@example.com");
    }

    @Test
    void loadUserByUsername_unknownEmail_throws() {
        given(userRepository.findByEmail("nobody@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
