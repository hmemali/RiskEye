package com.org.pfe.authen_service.controller;

import com.org.pfe.authen_service.dto.UpdateOwnProfileRequest;
import com.org.pfe.authen_service.dto.AuthResponse;
import com.org.pfe.authen_service.dto.UserResponse;
import com.org.pfe.authen_service.entity.User;
import com.org.pfe.authen_service.repository.UserRepository;
import com.org.pfe.authen_service.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/me/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @GetMapping
    public UserResponse getOwnProfile(Authentication authentication) {
        return toResponse(findCurrentUser(authentication));
    }

    @PutMapping
    public AuthResponse updateOwnProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateOwnProfileRequest request
    ) {
        User user = findCurrentUser(authentication);
        userRepository.findByEmail(request.email().trim())
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette adresse email est déjà utilisée.");
                });
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        User saved = userRepository.save(user);
        return new AuthResponse(
            jwtService.generateToken(saved.getEmail(), saved.getRole(), saved.getId()),
            saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole());
    }

    private User findCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}