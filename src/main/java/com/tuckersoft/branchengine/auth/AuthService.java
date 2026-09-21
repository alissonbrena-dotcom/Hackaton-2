package com.tuckersoft.branchengine.auth;

import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.security.JwtService;
import com.tuckersoft.branchengine.user.Role;
import com.tuckersoft.branchengine.user.User;
import com.tuckersoft.branchengine.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.conflict("El email " + request.email() + " ya esta registrado");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRole(Role.ROLE_USER);
        user.setCreatedAt(Instant.now());
        userRepository.save(user);
        return AuthResponse.of(jwtService.generateToken(user.getEmail()), user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Email inexistente y contrasena incorrecta terminan igual: BadCredentialsException -> 401
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));
        return AuthResponse.of(jwtService.generateToken(user.getEmail()), user);
    }
}
