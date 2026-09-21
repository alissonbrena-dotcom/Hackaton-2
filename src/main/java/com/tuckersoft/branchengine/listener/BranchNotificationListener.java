package com.tuckersoft.branchengine.listener;

import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * TEMPLATE for the classic hackathon requirement:
 *   "the controller returns 2xx immediately; email/notification happens
 *    afterwards, in a separate thread, only once the DB transaction that
 *    created the record has actually committed."
 *
 * Three things make this pattern work, all of which are easy to get wrong
 * under time pressure -- keep them:
 *
 * 1. @TransactionalEventListener(phase = AFTER_COMMIT), NOT @EventListener.
 *    A plain @EventListener fires INSIDE the publisher's transaction, before
 *    commit -- so if this listener re-reads the entity from the DB (which it
 *    should, see DecisionCommittedEvent's javadoc) it may not find it yet.
 *
 * 2. @Async on this method (backed by the ThreadPoolTaskExecutor from
 *    AsyncConfig) -- this is what actually moves the work off the request
 *    thread and onto a pool thread (check with a log line: the thread name
 *    should show your configured prefix, e.g. "app-worker-1", never
 *    "http-nio-8080-exec-*").
 *
 * 3. This class's OWN @Transactional(propagation = REQUIRES_NEW) on the
 *    method -- because AFTER_COMMIT runs after the original transaction is
 *    already closed, any DB writes this listener does (e.g. logging a
 *    NotificationLog row, updating a status column) need a brand new
 *    transaction of their own. Spring will actually refuse to start
 *    (BeanInitializationException at boot) if you combine
 *    @TransactionalEventListener with a plain @Transactional that doesn't
 *    explicitly say REQUIRES_NEW or NOT_SUPPORTED -- it's not just a style
 *    preference, the propagation must be explicit.
 *
 * The service that publishes the event must NEVER inject this class or the
 * mail sender directly -- publish the event via ApplicationEventPublisher
 * and let Spring find this @Component. That's what keeps the "immediate
 * 2xx" guarantee: publishing an event is fire-and-forget from the caller's
 * point of view.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BranchNotificationListener {

    private final EmailService emailService;
    // TODO inject your repository/repositories here to reload the entity
    // and persist whatever status/log row your spec requires.

    @Async("branchExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDecisionCommitted(DecisionCommittedEvent event) {
        String threadName = Thread.currentThread().getName();

        // TODO: reload the real entity by event.getEntityId(), build a real
        // subject/body, and persist a log row for the attempt/result, e.g.:
        //
        // MyEntity entity = repository.findById(event.getEntityId())
        //         .orElseThrow(() -> new IllegalStateException("not found after commit"));
        //
        // try {
        //     emailService.sendSimpleMessage(entity.getRecipientEmail(), subject, body);
        //     entity.setStatus(Status.SENT);
        // } catch (Exception e) {
        //     entity.setStatus(Status.FAILED);
        //     log.error("Notification failed for entity {}", event.getEntityId(), e);
        // }
        // repository.save(entity);

        log.info("[ASYNC-LOG] EntityId: {} | Thread: {} | (template listener - wire up real logic)",
                event.getEntityId(), threadName);
    }
}
