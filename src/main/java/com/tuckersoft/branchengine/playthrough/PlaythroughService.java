package com.tuckersoft.branchengine.playthrough;

import com.tuckersoft.branchengine.decision.Decision;
import com.tuckersoft.branchengine.decision.DecisionRepository;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.node.StoryNode;
import com.tuckersoft.branchengine.node.StoryNodeRepository;
import com.tuckersoft.branchengine.security.CurrentUserService;
import com.tuckersoft.branchengine.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaythroughService {

    private final PlaythroughRepository playthroughRepository;
    private final StoryNodeRepository storyNodeRepository;
    private final DecisionRepository decisionRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public PlaythroughResponse create(PlaythroughRequest request) {
        User user = currentUserService.getCurrentUser();
        StoryNode node = storyNodeRepository.findByNodeCodeForUpdate(request.startNodeCode())
                .orElseThrow(() -> ApiException.notFound("No existe el nodo " + request.startNodeCode()));
        if (playthroughRepository.existsByPlayerTag(request.playerTag())) {
            throw ApiException.conflict("Ya existe una partida con playerTag " + request.playerTag());
        }
        if (node.isFull()) {
            throw ApiException.badRequest("El nodo " + node.getNodeCode() + " esta lleno");
        }

        Instant now = Instant.now();
        Playthrough playthrough = new Playthrough();
        playthrough.setPlayerTag(request.playerTag());
        playthrough.setUser(user);
        playthrough.setCurrentNode(node);
        playthrough.setStartNodeCode(node.getNodeCode());
        playthrough.setLucidity(100);
        playthrough.setControlLevel(0);
        playthrough.setStatus(PlaythroughStatus.ACTIVA);
        playthrough.setEndingCode(null);
        playthrough.setCreatedAt(now);
        playthrough.setUpdatedAt(now);

        node.setCurrentBranches(node.getCurrentBranches() + 1);
        storyNodeRepository.save(node);

        return PlaythroughResponse.from(playthroughRepository.save(playthrough));
    }

    @Transactional(readOnly = true)
    public List<PlaythroughResponse> findAll() {
        User user = currentUserService.getCurrentUser();
        List<Playthrough> playthroughs = CurrentUserService.isAdmin(user)
                ? playthroughRepository.findAllByOrderByCreatedAtDesc()
                : playthroughRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return playthroughs.stream().map(PlaythroughResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PlaythroughResponse findById(Long id) {
        return PlaythroughResponse.from(findReadable(id));
    }

    @Transactional(readOnly = true)
    public PathResponse path(Long id) {
        Playthrough p = findReadable(id);
        List<Decision> decisions = decisionRepository
                .findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAscIdAsc(p.getId());
        List<PathResponse.Step> steps = new ArrayList<>();
        for (int i = 0; i < decisions.size(); i++) {
            Decision d = decisions.get(i);
            steps.add(new PathResponse.Step(i + 1, d.getId(), d.getNode().getNodeCode(), d.getResolvedNodeCode(),
                    d.getBranchType().name(), d.getImpactLevel().name(), d.getCreatedAt()));
        }
        return new PathResponse(p.getId(), p.getPlayerTag(), p.getStatus().name(), p.getEndingCode(),
                p.getStartNodeCode(), p.getCurrentNode().getNodeCode(), steps);
    }

    /** Lectura: el dueno o un admin (supervisor). El resto recibe 403. */
    private Playthrough findReadable(Long id) {
        User user = currentUserService.getCurrentUser();
        Playthrough p = playthroughRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No existe la partida " + id));
        if (!CurrentUserService.isAdmin(user) && !p.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("La partida " + id + " no te pertenece");
        }
        return p;
    }
}
