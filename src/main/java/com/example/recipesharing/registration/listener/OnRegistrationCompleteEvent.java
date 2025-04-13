package com.example.recipesharing.registration.listener;

import com.example.recipesharing.persistense.model.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Locale;

@Getter
public class OnRegistrationCompleteEvent extends ApplicationEvent {

    private final String appUrl;
    private final Locale locale;
    private final User user;
    private final String existingToken;

    public OnRegistrationCompleteEvent(User user, Locale locale, String appUrl, String existingToken) {
        super(user);
        this.user = user;
        this.locale = locale;
        this.appUrl = appUrl;
        this.existingToken = existingToken;
    }

}
