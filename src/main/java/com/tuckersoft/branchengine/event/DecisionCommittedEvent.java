package com.tuckersoft.branchengine.event;

/**
 * TEMPLATE. Rename/replace with your real domain event
 * (e.g. "TropelSignalCreatedEvent", "OrderPlacedEvent", ...).
 *
 * Keep it a plain data carrier: just enough info for the listener to reload
 * the entity from the DB. Don't put the entity itself in here if you can
 * avoid it -- by the time the listener runs (AFTER_COMMIT, on another
 * thread) you want a fresh read, not a detached JPA object from the
 * publishing transaction's persistence context.
 */
public class DecisionCommittedEvent {

    private final Long entityId;

    public DecisionCommittedEvent(Long entityId) {
        this.entityId = entityId;
    }

    public Long getEntityId() {
        return entityId;
    }
}
