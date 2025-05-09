package com.example.recipesharing.web.error;

import java.io.Serial;

public final class UserAlreadyExistException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 5861310537366287163L;

    public UserAlreadyExistException() {
        super();
    }

    public UserAlreadyExistException(final String message) {
        super(message);
    }

}
