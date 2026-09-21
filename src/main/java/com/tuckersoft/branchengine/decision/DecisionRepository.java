package com.tuckersoft.branchengine.decision;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DecisionRepository extends JpaRepository<Decision, Long>, JpaSpecificationExecutor<Decision> {

    @EntityGraph(attributePaths = {"node"})
    List<Decision> findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAscIdAsc(Long playthroughId);

    @Override
    @EntityGraph(attributePaths = {"playthrough", "node"})
    Page<Decision> findAll(Specification<Decision> spec, Pageable pageable);
}
