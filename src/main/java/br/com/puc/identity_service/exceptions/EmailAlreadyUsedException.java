package br.com.puc.identity_service.exceptions;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException(String msg) { super(msg); }
}
