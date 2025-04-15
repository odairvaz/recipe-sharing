package com.example.recipesharing.service.impl;

import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.persistense.model.VerificationToken;
import com.example.recipesharing.persistense.repository.IUserRepository;
import com.example.recipesharing.persistense.repository.IVerificationTokenRepository;
import com.example.recipesharing.service.ActivationResult;
import com.example.recipesharing.service.IUserService;
import com.example.recipesharing.web.dto.UserDto;
import com.example.recipesharing.web.error.UserAlreadyExistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class IUserServiceImpl implements IUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IUserServiceImpl.class);

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IVerificationTokenRepository tokenRepository;


    public IUserServiceImpl(IUserRepository userRepository, PasswordEncoder passwordEncoder, IVerificationTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User registerNewUserAccount(UserDto registrationRequest, String avatarUrl) {
        if (emailExists(registrationRequest.getEmail())) {
            throw new UserAlreadyExistException("There is an account with that email address: " + registrationRequest.getEmail());
        }
        final User user = new User();

        user.setFirstName(registrationRequest.getFirstName());
        user.setLastName(registrationRequest.getLastName());
        user.setBio(registrationRequest.getBio());
        user.setEmail(registrationRequest.getEmail());
        user.setAvatarUrl(avatarUrl);
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public void createVerificationToken(final User user, final String token) {
        final VerificationToken myToken = new VerificationToken(token, user, LocalDateTime.now().plusHours(24));
        tokenRepository.save(myToken);
    }

    @Override
    public void saveRegisteredUser(User user) {
        userRepository.save(user);
    }

    @Override
    public ActivationResult activateUserByToken(String token) {
        Optional<VerificationToken> optToken = tokenRepository.findByToken(token);

        if (optToken.isEmpty()) {
            return ActivationResult.TOKEN_INVALID;
        }

        VerificationToken verificationToken = optToken.get();
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ActivationResult.TOKEN_EXPIRED;
        }

        User user = verificationToken.getUser();
        if (user == null) {
            LOGGER.error("Verification token: {} has no associated user.", token);
            return ActivationResult.TOKEN_INVALID;
        }

        if (user.isEnabled()) {
            return ActivationResult.ALREADY_ACTIVE;
        }

        user.setEnabled(true);
        userRepository.save(user);
        return ActivationResult.SUCCESS;
    }

    private boolean emailExists(final String email) {
        return userRepository.findByEmail(email).isPresent();
    }

}
