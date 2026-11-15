package com.example.recipesharing.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.passay.*;

import java.util.Arrays;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private static final PasswordValidator VALIDATOR = new PasswordValidator(Arrays.asList(
            new LengthRule(8, 30),
            new CharacterRule(EnglishCharacterData.UpperCase, 1),
            new CharacterRule(EnglishCharacterData.Digit, 1),
            new CharacterRule(EnglishCharacterData.Special, 1),
            new IllegalSequenceRule(EnglishSequenceData.Numerical, 3, false),
            new IllegalSequenceRule(EnglishSequenceData.Alphabetical, 3, false),
            new IllegalSequenceRule(EnglishSequenceData.USQwerty, 3, false),
            new WhitespaceRule()));

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        final RuleResult result = VALIDATOR.validate(new PasswordData(password));
        if (result.isValid()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(String.join(" ", VALIDATOR.getMessages(result))).addConstraintViolation();
        return false;
    }
}
