package com.example.recipesharing.service;

import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.web.dto.UserDto;
import com.example.recipesharing.web.error.UserAlreadyExistException;

public interface IUserService {

    User registerNewUserAccount(UserDto userDto) throws UserAlreadyExistException;

    void createVerificationToken(final User user, final String token);

    void saveRegisteredUser(User user);

    ActivationResult activateUserByToken(String token);

}
