package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.node.StoryNode;
import com.tuckersoft.branchengine.node.StoryNodeRepository;
import com.tuckersoft.branchengine.notification.RealityLogRepository;
import com.tuckersoft.branchengine.notification.RealityLogResponse;
import com.tuckersoft.branchengine.playthrough.Playthrough;
import com.tuckersoft.branchengine.playthrough.PlaythroughRepository;
import com.tuckersoft.branchengine.playthrough.PlaythroughStatus;
import com.tuckersoft.branchengine.security.CurrentUserService;
import com.tuckersoft.branchengine.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.tuckersoft.branchengine.decision.DecisionSpecifications.*;

@Service
@RequiredArgsConstructor
public class DecisionService {

    public static final String SIMULATE_MAIL_FAILURE = "MAIL_FAILURE";

    static final String ENDING_PAC_SYMBOL = "ENDING_PAC_SYMBOL";
    static final String ENDING_WHITE_BEAR = "ENDING_WHITE_BEAR";
    static final String ENDING_NETFLIX_CUT = "ENDING_NETFLIX_CUT";

    private final DecisionRepository decisionRepository;
    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final RealityLogRepository realityLogRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DecisionResponse create(DecisionRequest request, String simulateHeader) {
        // 1. Usuario del token y propiedad de la partida (ni el admin decide sobre partidas ajenas)
        User user = currentUserService.getCurrentUser();
        Playthrough playthrough = playthroughRepository.findById(request.playthroughId())
                .orElseThrow(() -> ApiException.notFound("No existe la partida " + request.playthroughId()));
        if (!playthrough.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("Solo el dueno de la partida puede decidir sobre ella");
        }

        // 2. La partida tiene que seguir ACTIVA
        if (playthrough.getStatus() == PlaythroughStatus.FINALIZADA) {
            throw ApiException.conflict("La partida " + playthrough.getId() + " ya esta FINALIZADA");
        }

        // 3. Clasificar y derivar handlerUnit / outcomeCode
        ImpactLevel impact = ImpactLevel.valueOf(request.impactLevel());
        BranchType branchType = BranchClassifier.classify(request.rawInput());
        StoryNode source = playthrough.getCurrentNode();
        Instant now = Instant.now();

        Decision decision = new Decision();
        decision.setPlaythrough(playthrough);
        decision.setNode(source);
        decision.setRawInput(request.rawInput());
        decision.setBranchType(branchType);
        decision.setImpactLevel(impact);
        decision.setHandlerUnit(branchType.getHandlerUnit());
        decision.setOutcomeCode(branchType.getOutcomeCode());
        decision.setCreatedAt(now);
        decision.setUpdatedAt(now);

        // 4. Entrada corrupta: se guarda en ERROR, sin tocar la partida ni publicar evento
        if (branchType == BranchType.ENTRADA_CORRUPTA) {
            decision.setResolvedNodeCode(null);
            decision.setStatus(DecisionStatus.ERROR);
            return DecisionResponse.from(decisionRepository.save(decision));
        }

        // 5. Stats, nodo destino y estado de la partida
        int lucidity = clamp(playthrough.getLucidity() + impact.getLucidityDelta());
        int controlLevel = clamp(playthrough.getControlLevel() + impact.getControlDelta());
        playthrough.setLucidity(lucidity);
        playthrough.setControlLevel(controlLevel);

        boolean glitch = branchType == BranchType.RUPTURA_CUARTA_PARED || impact == ImpactLevel.CRITICO;
        String targetCode = glitch ? source.getGlitchBranchCode() : source.getPrimaryBranchCode();
        decision.setResolvedNodeCode(targetCode);

        // El orden importa: el control gana a la lucidez
        if (controlLevel >= 100) {
            finish(playthrough, ENDING_PAC_SYMBOL);
        } else if (lucidity <= 0) {
            finish(playthrough, ENDING_WHITE_BEAR);
        } else {
            Optional<StoryNode> target = targetCode == null
                    ? Optional.empty()
                    : storyNodeRepository.findByNodeCode(targetCode);
            if (target.isEmpty()) {
                finish(playthrough, ENDING_NETFLIX_CUT);
            } else {
                playthrough.setStatus(PlaythroughStatus.ACTIVA);
                playthrough.setCurrentNode(target.get());
            }
        }
        playthrough.setUpdatedAt(now);

        // 6-7. Guardar partida y decision
        playthroughRepository.save(playthrough);
        decision.setStatus(DecisionStatus.REGISTRADA);
        Decision saved = decisionRepository.save(decision);

        // 8. Publicar el evento: el listener solo corre despues del COMMIT
        eventPublisher.publishEvent(buildEvent(saved, playthrough, simulateHeader));

        // 9. El controller responde 201
        return DecisionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<DecisionResponse> findAll(BranchType branchType, ImpactLevel impactLevel,
                                                  DecisionStatus status, Long playthroughId, int page, int size) {
        if (page < 0 || size < 1) {
            throw ApiException.badRequest("page debe ser >= 0 y size >= 1");
        }
        User user = currentUserService.getCurrentUser();
        Long ownerFilter = CurrentUserService.isAdmin(user) ? null : user.getId();
        Specification<Decision> spec = Specification.where(ownedBy(ownerFilter))
                .and(hasBranchType(branchType))
                .and(hasImpactLevel(impactLevel))
                .and(hasStatus(status))
                .and(inPlaythrough(playthroughId));
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageResponse.from(decisionRepository.findAll(spec, pageable).map(DecisionResponse::from));
    }

    @Transactional(readOnly = true)
    public DecisionResponse findById(Long id) {
        return DecisionResponse.from(findReadable(id));
    }

    @Transactional(readOnly = true)
    public List<RealityLogResponse> findRealityLogs(Long id) {
        Decision decision = findReadable(id);
        return realityLogRepository.findByDecisionIdOrderByCreatedAtAsc(decision.getId()).stream()
                .map(RealityLogResponse::from)
                .toList();
    }

    /** Lectura: el dueno de la partida o un admin. El resto recibe 403. */
    private Decision findReadable(Long id) {
        User user = currentUserService.getCurrentUser();
        Decision decision = decisionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No existe la decision " + id));
        boolean owner = decision.getPlaythrough().getUser().getId().equals(user.getId());
        if (!owner && !CurrentUserService.isAdmin(user)) {
            throw ApiException.forbidden("La decision " + id + " no te pertenece");
        }
        return decision;
    }

    private static void finish(Playthrough playthrough, String endingCode) {
        playthrough.setStatus(PlaythroughStatus.FINALIZADA);
        playthrough.setEndingCode(endingCode);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static DecisionCommittedEvent buildEvent(Decision d, Playthrough p, String simulateHeader) {
        User owner = p.getUser();
        return new DecisionCommittedEvent(
                d.getId(), owner.getEmail(), owner.getDisplayName(), p.getPlayerTag(),
                d.getBranchType().name(), d.getImpactLevel().name(), d.getHandlerUnit(), d.getOutcomeCode(),
                d.getNode().getNodeCode(), d.getResolvedNodeCode(), p.getStatus().name(),
                p.getLucidity(), p.getControlLevel(), p.getEndingCode(), d.getCreatedAt(), d.getRawInput(),
                SIMULATE_MAIL_FAILURE.equals(simulateHeader));
    }
}
