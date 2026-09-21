package com.tuckersoft.branchengine.notification;

import com.tuckersoft.branchengine.decision.Decision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Auditoria de cada intento de envio del Informe de Realidad. */
@Entity
@Table(name = "reality_logs")
@Getter
@Setter
@NoArgsConstructor
public class RealityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "decision_id", nullable = false)
    private Decision decision;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogStatus logStatus;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Instant sentAt;

    @Column(nullable = false)
    private Instant createdAt;
}
