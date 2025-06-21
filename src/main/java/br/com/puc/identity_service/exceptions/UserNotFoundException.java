package br.com.puc.identity_service.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String msg) { super(msg); }
}
