package com.kairon.exception;

public class ClientAlreadyExistsException extends BusinessException {

    public ClientAlreadyExistsException(String phone) {
        super("Client with phone " + phone + " already exists");
    }

    public ClientAlreadyExistsException(String phone, String companyId) {
        super("Client with phone " + phone + " already exists in company: " + companyId);
    }
}