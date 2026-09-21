package com.tuckersoft.branchengine.notification;

import com.tuckersoft.branchengine.decision.Decision;
import com.tuckersoft.branchengine.decision.DecisionCommittedEvent;
import com.tuckersoft.branchengine.decision.DecisionRepository;
import com.tuckersoft.branchengine.decision.DecisionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

/**
 * Envia el Informe de Realidad en un hilo branch-worker, solo despues de que PostgreSQL
 * confirmo la decision (AFTER_COMMIT). Con @EventListener el listener podria buscar una
 * decision que todavia no existe en la base de datos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BranchNotificationListener {

    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;
    private final JavaMailSender mailSender;

    @Async("branchExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCommit(DecisionCommittedEvent event) {
        Decision decision = decisionRepository.findById(event.decisionId()).orElse(null);
        if (decision == null) {
            log.error("[BRANCH-LOG] La decision {} no existe, no se envia el informe", event.decisionId());
            return;
        }
        decision.setStatus(DecisionStatus.PROCESANDO);
        decision.setUpdatedAt(Instant.now());

        String subject = subject(event);
        RealityLog realityLog = new RealityLog();
        realityLog.setDecision(decision);
        realityLog.setRecipientEmail(event.recipientEmail());
        realityLog.setSubject(subject);

        try {
            send(event, subject);
            decision.setStatus(DecisionStatus.ESTABILIZADA);
            realityLog.setLogStatus(LogStatus.SENT);
            realityLog.setSentAt(Instant.now());
        } catch (Exception e) {
            log.error("No se pudo enviar el Informe de Realidad de la decision {}: {}",
                    event.decisionId(), e.getMessage(), e);
            decision.setStatus(DecisionStatus.ERROR);
            realityLog.setLogStatus(LogStatus.FAILED);
            realityLog.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
        }

        Instant now = Instant.now();
        decision.setUpdatedAt(now);
        realityLog.setCreatedAt(now);
        decisionRepository.save(decision);
        realityLogRepository.save(realityLog);

        log.info("[BRANCH-LOG] Decision ID: {} | Player: {} | Branch: {} | Impact: {} | Unit: {} | Node: {} -> {} "
                        + "| Thread: {} | Status: {}",
                event.decisionId(), event.playerTag(), event.branchType(), event.impactLevel(),
                event.handlerUnit(), event.sourceNodeCode(), event.resolvedNodeCode(),
                Thread.currentThread().getName(), decision.getStatus());
    }

    private void send(DecisionCommittedEvent event, String subject) {
        if (event.simulateMailFailure()) {
            // Modo QA: falla real, atrapada por el mismo catch que un fallo SMTP de verdad
            throw new MailSendException("Fallo SMTP simulado (X-Bandersnatch-Simulate: MAIL_FAILURE)");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.recipientEmail());
        message.setSubject(subject);
        message.setText(body(event));
        mailSender.send(message);
    }

    static String subject(DecisionCommittedEvent e) {
        return "[TUCKERSOFT] " + e.branchType() + " en " + e.playerTag() + " | Impacto " + e.impactLevel();
    }

    static String body(DecisionCommittedEvent e) {
        return """
                Hola %s,

                Una partida de prueba acaba de ramificarse.

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                Decision ID      : #%d
                Jugador          : %s
                Rama             : %s
                Impacto          : %s
                Departamento     : %s
                Consecuencia     : %s
                Nodo origen      : %s
                Nodo destino     : %s
                Estado partida   : %s
                Lucidez          : %d/100
                Nivel de control : %d/100
                Final            : %s
                Registrada       : %s
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                Decisión original del jugador:
                "%s"

                — Tuckersoft Branch Engine, 1984
                """.formatted(
                e.displayName(), e.decisionId(), e.playerTag(), e.branchType(), e.impactLevel(),
                e.handlerUnit(), e.outcomeCode(), e.sourceNodeCode(), orDash(e.resolvedNodeCode()),
                e.playthroughStatus(), e.lucidity(), e.controlLevel(), orDash(e.endingCode()),
                e.createdAt(), e.rawInput());
    }

    private static String orDash(String value) {
        return value == null ? "-" : value;
    }
}
