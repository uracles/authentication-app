package com.miracle.app.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserProfileResponse {

    private String userId;
    private String username;
    private List<String> roles;
}