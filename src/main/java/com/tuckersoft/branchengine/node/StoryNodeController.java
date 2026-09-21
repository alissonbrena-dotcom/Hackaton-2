package com.tuckersoft.branchengine.node;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class StoryNodeController {

    private final StoryNodeService storyNodeService;

    @PostMapping
    public ResponseEntity<StoryNodeResponse> create(@Valid @RequestBody StoryNodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storyNodeService.create(request));
    }

    @GetMapping
    public List<StoryNodeResponse> findAll() {
        return storyNodeService.findAll();
    }

    @GetMapping("/{id}")
    public StoryNodeResponse findById(@PathVariable Long id) {
        return storyNodeService.findById(id);
    }
}
