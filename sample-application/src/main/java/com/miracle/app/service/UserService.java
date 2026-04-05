package com.miracle.app.service;

import com.miracle.app.model.UserProfileResponse;
import com.miracle.app.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getMyProfile(UserDetails principal) {
        var user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found in database: " + principal.getUsername()));

        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                roles
        );
    }
}