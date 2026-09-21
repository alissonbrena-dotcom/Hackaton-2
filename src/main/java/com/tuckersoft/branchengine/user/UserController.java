package com.tuckersoft.branchengine.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me() {
        return userService.me();
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return userService.changeRole(id, request.role());
    }
}
