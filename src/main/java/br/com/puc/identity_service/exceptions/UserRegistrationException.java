package br.com.puc.identity_service.exceptions;

public class UserRegistrationException extends RuntimeException {
    public UserRegistrationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
