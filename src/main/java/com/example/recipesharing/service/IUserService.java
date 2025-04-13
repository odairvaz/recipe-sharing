package com.example.recipesharing.service;

import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.persistense.model.VerificationToken;
import com.example.recipesharing.web.dto.UserDto;
import com.example.recipesharing.web.error.UserAlreadyExistException;

import java.util.Optional;

public interface IUserService {

    User registerNewUserAccount(UserDto userDto) throws UserAlreadyExistException;

    void createVerificationToken(final User user, final String token);

    Optional<VerificationToken> getVerificationToken(String verificationToken);

    void saveRegisteredUser(User user);

}
