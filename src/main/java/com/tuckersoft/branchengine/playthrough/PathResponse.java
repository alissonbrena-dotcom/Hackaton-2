package com.tuckersoft.branchengine.playthrough;

import java.time.Instant;
import java.util.List;

public record PathResponse(Long playthroughId, String playerTag, String status, String endingCode,
                           String startNodeCode, String currentNodeCode, List<Step> steps) {

    public record Step(int order, Long decisionId, String fromNodeCode, String toNodeCode,
                       String branchType, String impactLevel, Instant createdAt) {
    }
}
