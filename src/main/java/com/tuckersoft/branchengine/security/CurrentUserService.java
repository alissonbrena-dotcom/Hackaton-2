package com.tuckersoft.branchengine.security;

import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.user.Role;
import com.tuckersoft.branchengine.user.User;
import com.tuckersoft.branchengine.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** El usuario autenticado siempre sale del token (SecurityContext), nunca del request body. */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "No hay usuario autenticado");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                        "El usuario del token ya no existe"));
    }

    public static boolean isAdmin(User user) {
        return user.getRole() == Role.ROLE_ADMIN;
    }
}
