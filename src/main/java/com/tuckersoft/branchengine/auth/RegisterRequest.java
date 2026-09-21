package com.tuckersoft.branchengine.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** No tiene campo role: si el JSON lo trae, Jackson lo ignora y el rol lo fija el service. */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank @Size(min = 3, max = 60) String displayName) {
}
