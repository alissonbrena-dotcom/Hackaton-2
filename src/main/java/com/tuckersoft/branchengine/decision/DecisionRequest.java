package com.tuckersoft.branchengine.decision;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DecisionRequest(
        @NotNull Long playthroughId,
        @NotBlank @Size(min = 10) String rawInput,
        @NotBlank @Pattern(regexp = "LEVE|MODERADO|GRAVE|CRITICO",
                message = "debe ser LEVE, MODERADO, GRAVE o CRITICO") String impactLevel) {
}
