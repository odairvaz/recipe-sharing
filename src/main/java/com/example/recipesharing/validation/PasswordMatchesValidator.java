package com.example.recipesharing.validation;

import com.example.recipesharing.web.dto.PasswordDto;
import com.example.recipesharing.web.dto.UserDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    private String message;

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        boolean passwordsMatch = false;

        if (obj instanceof UserDto userDto) {
            passwordsMatch = userDto.getPassword().equals(userDto.getMatchingPassword());
        } else if (obj instanceof PasswordDto passwordDto) {
            passwordsMatch = passwordDto.getPassword().equals(passwordDto.getMatchingPassword());
        }

        if (!passwordsMatch) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode("password")
                    .addConstraintViolation();
        }

        return passwordsMatch;
    }


}
