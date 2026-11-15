package com.example.recipesharing.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private String message;

    @Override
    public void initialize(ValidEmail constraintAnnotation) {
        message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        // null is left to @NotNull
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode("email")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
