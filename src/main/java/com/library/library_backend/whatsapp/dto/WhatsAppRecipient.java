package com.library.library_backend.whatsapp.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class WhatsAppRecipient {

    private String phoneNumber;
    private String name;
    private Map<String, Object> variables = new LinkedHashMap<>();
    private Long id;

    public WhatsAppRecipient() {
    }

    public WhatsAppRecipient(String phoneNumber, Map<String, Object> variables) {
        this.phoneNumber = phoneNumber;
        this.variables = variables != null ? variables : new LinkedHashMap<>();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
