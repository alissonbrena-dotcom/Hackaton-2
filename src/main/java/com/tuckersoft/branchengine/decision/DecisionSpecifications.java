package com.tuckersoft.branchengine.decision;

import org.springframework.data.jpa.domain.Specification;

/** Filtros de GET /api/v1/decisions. Un filtro nulo no restringe nada. */
final class DecisionSpecifications {

    private DecisionSpecifications() {
    }

    static Specification<Decision> ownedBy(Long userId) {
        return (root, query, cb) -> userId == null ? null
                : cb.equal(root.get("playthrough").get("user").get("id"), userId);
    }

    static Specification<Decision> hasBranchType(BranchType branchType) {
        return (root, query, cb) -> branchType == null ? null : cb.equal(root.get("branchType"), branchType);
    }

    static Specification<Decision> hasImpactLevel(ImpactLevel impactLevel) {
        return (root, query, cb) -> impactLevel == null ? null : cb.equal(root.get("impactLevel"), impactLevel);
    }

    static Specification<Decision> hasStatus(DecisionStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    static Specification<Decision> inPlaythrough(Long playthroughId) {
        return (root, query, cb) -> playthroughId == null ? null
                : cb.equal(root.get("playthrough").get("id"), playthroughId);
    }
}
