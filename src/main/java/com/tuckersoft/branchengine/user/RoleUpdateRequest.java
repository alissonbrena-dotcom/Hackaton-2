package com.tuckersoft.branchengine.user;

import jakarta.validation.constraints.NotBlank;

public record RoleUpdateRequest(@NotBlank String role) {
}
