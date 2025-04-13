package com.example.recipesharing.service.impl;

import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.persistense.model.VerificationToken;
import com.example.recipesharing.persistense.repository.IUserRepository;
import com.example.recipesharing.persistense.repository.VerificationTokenRepository;
import com.example.recipesharing.service.IUserService;
import com.example.recipesharing.web.dto.UserDto;
import com.example.recipesharing.web.error.UserAlreadyExistException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class IUserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository tokenRepository;


    public IUserServiceImpl(IUserRepository userRepository, PasswordEncoder passwordEncoder, VerificationTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User registerNewUserAccount(UserDto userDto) {
        if (emailExists(userDto.getEmail())) {
            throw new UserAlreadyExistException("There is an account with that email address: " + userDto.getEmail());
        }
        final User user = new User();

        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setBio(userDto.getBio());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setEmail(userDto.getEmail());
        return userRepository.save(user);
    }

    @Override
    public void createVerificationToken(final User user, final String token) {
        final VerificationToken myToken = new VerificationToken(token, user, LocalDateTime.now().plusHours(24));
        tokenRepository.save(myToken);
    }

    @Override
    public Optional<VerificationToken> getVerificationToken(String verificationToken) {
        return Optional.ofNullable(tokenRepository.findByToken(verificationToken));
    }

    @Override
    public void saveRegisteredUser(User user) {
        userRepository.save(user);
    }

    private boolean emailExists(final String email) {
        return userRepository.findByEmail(email).isPresent();
    }

}
