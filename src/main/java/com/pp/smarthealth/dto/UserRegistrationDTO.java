package com.pp.smarthealth.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserRegistrationDTO {
    private String username;
    private String password;
    private String email;
    private List<String> roles; // e.g., ["ROLE_PATIENT", "ROLE_DOCTOR"]
}
