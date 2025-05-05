package com.example.recipesharing.service;

import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.web.dto.UserDto;
import com.example.recipesharing.web.error.UserAlreadyExistException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface IUserService {

    User registerNewUserAccount(UserDto userDto, MultipartFile avatarFile) throws UserAlreadyExistException;

    void createVerificationToken(final User user, final String token);

    void saveRegisteredUser(User user);

    ActivationResult activateUserByToken(String token);

    Optional<User> findUserByEmail(String email);

}
