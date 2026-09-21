package com.tuckersoft.branchengine.auth;

import com.tuckersoft.branchengine.user.User;

public record AuthResponse(String token, String type, String email, String displayName, String role) {

    public static AuthResponse of(String token, User user) {
        return new AuthResponse(token, "Bearer", user.getEmail(), user.getDisplayName(), user.getRole().name());
    }
}
