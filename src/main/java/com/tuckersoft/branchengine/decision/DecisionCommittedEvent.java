package com.tuckersoft.branchengine.decision;

import java.time.Instant;

/**
 * Todo lo que el listener necesita viaja aqui: en el hilo branch-worker ya no hay
 * usuario autenticado ni sesion de Hibernate de la peticion.
 */
public record DecisionCommittedEvent(
        Long decisionId,
        String recipientEmail,
        String displayName,
        String playerTag,
        String branchType,
        String impactLevel,
        String handlerUnit,
        String outcomeCode,
        String sourceNodeCode,
        String resolvedNodeCode,
        String playthroughStatus,
        int lucidity,
        int controlLevel,
        String endingCode,
        Instant createdAt,
        String rawInput,
        boolean simulateMailFailure) {
}
