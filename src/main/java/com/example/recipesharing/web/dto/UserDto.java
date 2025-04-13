package com.example.recipesharing.web.dto;


import com.example.recipesharing.validation.PasswordMatches;
import com.example.recipesharing.validation.ValidEmail;
import com.example.recipesharing.validation.ValidPassword;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@PasswordMatches
public class UserDto {
    @NotNull
    @NotEmpty
    private String firstName;

    @NotNull
    @NotEmpty
    private String lastName;

    @ValidPassword
    private String password;
    private String matchingPassword;

    @ValidEmail
    @NotNull
    @NotEmpty
    private String email;

    private String bio;

    private String avatarUrl;

}
