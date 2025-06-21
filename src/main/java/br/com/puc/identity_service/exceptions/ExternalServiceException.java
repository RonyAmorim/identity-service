package br.com.puc.identity_service.exceptions;

public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
