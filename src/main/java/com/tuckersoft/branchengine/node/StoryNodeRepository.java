package com.tuckersoft.branchengine.node;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoryNodeRepository extends JpaRepository<StoryNode, Long> {

    Optional<StoryNode> findByNodeCode(String nodeCode);

    boolean existsByNodeCode(String nodeCode);

    List<StoryNode> findAllByOrderByIdAsc();

    /** Bloquea la fila para que dos partidas simultaneas no superen branchCapacity. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from StoryNode n where n.nodeCode = :nodeCode")
    Optional<StoryNode> findByNodeCodeForUpdate(@Param("nodeCode") String nodeCode);
}
