package com.tuckersoft.branchengine.node;

import com.tuckersoft.branchengine.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryNodeService {

    private final StoryNodeRepository storyNodeRepository;

    @Transactional
    public StoryNodeResponse create(StoryNodeRequest request) {
        if (storyNodeRepository.existsByNodeCode(request.nodeCode())) {
            throw ApiException.conflict("Ya existe un nodo con nodeCode " + request.nodeCode());
        }
        StoryNode node = new StoryNode();
        node.setNodeCode(request.nodeCode());
        node.setTitle(request.title());
        node.setSceneText(request.sceneText());
        node.setBranchCapacity(request.branchCapacity());
        node.setCurrentBranches(0);
        node.setPrimaryBranchCode(request.primaryBranchCode());
        node.setGlitchBranchCode(request.glitchBranchCode());
        node.setCreatedAt(Instant.now());
        return StoryNodeResponse.from(storyNodeRepository.save(node));
    }

    @Transactional(readOnly = true)
    public List<StoryNodeResponse> findAll() {
        return storyNodeRepository.findAllByOrderByIdAsc().stream().map(StoryNodeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StoryNodeResponse findById(Long id) {
        return storyNodeRepository.findById(id)
                .map(StoryNodeResponse::from)
                .orElseThrow(() -> ApiException.notFound("No existe el nodo " + id));
    }
}
