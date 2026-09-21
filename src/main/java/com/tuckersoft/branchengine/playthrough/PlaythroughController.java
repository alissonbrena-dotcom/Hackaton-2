package com.tuckersoft.branchengine.playthrough;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playthroughs")
@RequiredArgsConstructor
public class PlaythroughController {

    private final PlaythroughService playthroughService;

    @PostMapping
    public ResponseEntity<PlaythroughResponse> create(@Valid @RequestBody PlaythroughRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playthroughService.create(request));
    }

    @GetMapping
    public List<PlaythroughResponse> findAll() {
        return playthroughService.findAll();
    }

    @GetMapping("/{id}")
    public PlaythroughResponse findById(@PathVariable Long id) {
        return playthroughService.findById(id);
    }

    @GetMapping("/{id}/path")
    public PathResponse path(@PathVariable Long id) {
        return playthroughService.path(id);
    }
}
