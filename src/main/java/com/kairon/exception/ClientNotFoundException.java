package com.kairon.exception;

public class ClientNotFoundException extends BusinessException {

    public ClientNotFoundException(String clientId) {
        super("Client not found with ID: " + clientId);
    }

    public ClientNotFoundException(String clientId, String companyId) {
        super("Client not found with ID: " + clientId + " for company: " + companyId);
    }
}