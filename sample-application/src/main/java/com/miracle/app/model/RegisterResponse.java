package com.miracle.app.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RegisterResponse {

    private String userId;
    private String username;
    private List<String> roles;
    private String message;
}