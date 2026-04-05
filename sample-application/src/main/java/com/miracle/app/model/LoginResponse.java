package com.miracle.app.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private String userId;
    private String username;
    private List<String> roles;

    public LoginResponse(String accessToken, String userId, String username, List<String> roles) {
        this.accessToken = accessToken;
        this.userId      = userId;
        this.username    = username;
        this.roles       = roles;
    }
}