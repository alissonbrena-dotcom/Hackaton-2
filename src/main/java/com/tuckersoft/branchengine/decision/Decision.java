package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.node.StoryNode;
import com.tuckersoft.branchengine.notification.RealityLog;
import com.tuckersoft.branchengine.playthrough.Playthrough;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decisions")
@Getter
@Setter
@NoArgsConstructor
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playthrough_id", nullable = false)
    private Playthrough playthrough;

    /** Nodo de origen: el currentNode de la partida al momento de decidir. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private StoryNode node;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawInput;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BranchType branchType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImpactLevel impactLevel;

    @Column(nullable = false)
    private String handlerUnit;

    @Column(nullable = false)
    private String outcomeCode;

    private String resolvedNodeCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "decision")
    private List<RealityLog> realityLogs = new ArrayList<>();
}
