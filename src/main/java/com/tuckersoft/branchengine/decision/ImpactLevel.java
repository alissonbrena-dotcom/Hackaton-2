package com.tuckersoft.branchengine.decision;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImpactLevel {
    LEVE(-5, 5),
    MODERADO(-15, 10),
    GRAVE(-30, 20),
    CRITICO(-40, 45);

    private final int lucidityDelta;
    private final int controlDelta;
}
