package com.tuckersoft.branchengine.node;

import java.time.Instant;

public record StoryNodeResponse(Long id, String nodeCode, String title, String sceneText,
                                Integer branchCapacity, Integer currentBranches,
                                String primaryBranchCode, String glitchBranchCode, Instant createdAt) {

    public static StoryNodeResponse from(StoryNode node) {
        return new StoryNodeResponse(node.getId(), node.getNodeCode(), node.getTitle(), node.getSceneText(),
                node.getBranchCapacity(), node.getCurrentBranches(), node.getPrimaryBranchCode(),
                node.getGlitchBranchCode(), node.getCreatedAt());
    }
}
