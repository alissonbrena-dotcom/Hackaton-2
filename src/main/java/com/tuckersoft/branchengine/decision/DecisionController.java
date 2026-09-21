package com.tuckersoft.branchengine.decision;

import com.tuckersoft.branchengine.notification.RealityLogResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decisions")
@RequiredArgsConstructor
public class DecisionController {

    private final DecisionService decisionService;

    @PostMapping
    public ResponseEntity<DecisionResponse> create(
            @Valid @RequestBody DecisionRequest request,
            @RequestHeader(value = "X-Bandersnatch-Simulate", required = false) String simulate) {
        return ResponseEntity.status(HttpStatus.CREATED).body(decisionService.create(request, simulate));
    }

    @GetMapping
    public PageResponse<DecisionResponse> findAll(
            @RequestParam(required = false) BranchType branchType,
            @RequestParam(required = false) ImpactLevel impactLevel,
            @RequestParam(required = false) DecisionStatus status,
            @RequestParam(required = false) Long playthroughId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return decisionService.findAll(branchType, impactLevel, status, playthroughId, page, size);
    }

    @GetMapping("/{id}")
    public DecisionResponse findById(@PathVariable Long id) {
        return decisionService.findById(id);
    }

    @GetMapping("/{id}/reality-logs")
    public List<RealityLogResponse> realityLogs(@PathVariable Long id) {
        return decisionService.findRealityLogs(id);
    }
}
