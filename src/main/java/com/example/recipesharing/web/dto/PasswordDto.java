package com.example.recipesharing.web.dto;

import com.example.recipesharing.validation.PasswordMatches;

@PasswordMatches
public class PasswordDto {

    @com.example.recipesharing.validation.ValidPassword
    private String password;

    private String matchingPassword;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMatchingPassword() {
        return matchingPassword;
    }

    public void setMatchingPassword(String matchingPassword) {
        this.matchingPassword = matchingPassword;
    }
}
