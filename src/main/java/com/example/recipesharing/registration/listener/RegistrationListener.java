package com.example.recipesharing.registration.listener;


import com.example.recipesharing.persistense.model.User;
import com.example.recipesharing.service.IUserService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.UUID;

@Component
public class RegistrationListener implements ApplicationListener<OnRegistrationCompleteEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationListener.class);

    private final IUserService userService;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public RegistrationListener(IUserService userService, JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.userService = userService;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void onApplicationEvent(OnRegistrationCompleteEvent event) {
        handleRegistrationConfirmation(event);
    }


    private void handleRegistrationConfirmation(OnRegistrationCompleteEvent event) {
        User user = event.getUser();
        String token = obtainVerificationToken(user, event.getExistingToken());
        String confirmationUrl = buildConfirmationUrl(event.getAppUrl(), token);
        String emailContent = generateEmailContent(user, event.getLocale(), confirmationUrl);
        sendEmail(user.getEmail(), emailContent);
    }

    private String obtainVerificationToken(User user, String existingToken) {
        if (existingToken == null) {
            String newToken = UUID.randomUUID().toString();
            userService.createVerificationToken(user, newToken);
            return newToken;
        }
        return existingToken;
    }

    private String buildConfirmationUrl(String baseUrl, String token) {
        return baseUrl + "/api/register/confirm?token=" + token;
    }

    private String generateEmailContent(User user, Locale locale, String confirmationUrl) {
        Context context = new Context(locale);
        context.setVariable("user", user);
        context.setVariable("confirmationUrl", confirmationUrl);
        return templateEngine.process("registration/email", context);
    }

    private void sendEmail(String recipient, String emailContent) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(recipient);
            helper.setSubject("Registration Confirmation");
            helper.setText(emailContent, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            LOGGER.error("Unable to send email: ", ex);
        }
    }

}
