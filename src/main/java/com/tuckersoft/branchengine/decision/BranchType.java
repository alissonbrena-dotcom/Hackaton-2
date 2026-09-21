package com.tuckersoft.branchengine.decision;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Cada rama sabe que departamento la atiende y que consecuencia produce. */
@Getter
@RequiredArgsConstructor
public enum BranchType {
    OBEDIENCIA("Mesa de Guion", "ADVANCE_MAIN_PATH"),
    REBELDIA("Control de Continuidad", "FORK_TIMELINE"),
    SOSPECHA("Oficina de Seguridad", "INJECT_WHITE_BEAR_SYMBOL"),
    RUPTURA_CUARTA_PARED("Departamento Netflix", "BREAK_FOURTH_WALL"),
    ENTRADA_CORRUPTA("Archivo de Errores", "DISCARD_INPUT");

    private final String handlerUnit;
    private final String outcomeCode;
}
