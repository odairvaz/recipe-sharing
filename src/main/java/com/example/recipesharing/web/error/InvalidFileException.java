package com.example.recipesharing.web.error;

public class InvalidFileException extends RuntimeException {

    public InvalidFileException() {
        super();
    }

    public InvalidFileException(String message) {
        super(message);
    }
}
