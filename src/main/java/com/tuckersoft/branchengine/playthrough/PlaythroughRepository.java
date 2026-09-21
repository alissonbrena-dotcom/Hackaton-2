package com.tuckersoft.branchengine.playthrough;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaythroughRepository extends JpaRepository<Playthrough, Long> {

    boolean existsByPlayerTag(String playerTag);

    @EntityGraph(attributePaths = {"user", "currentNode"})
    List<Playthrough> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "currentNode"})
    List<Playthrough> findAllByOrderByCreatedAtDesc();
}
