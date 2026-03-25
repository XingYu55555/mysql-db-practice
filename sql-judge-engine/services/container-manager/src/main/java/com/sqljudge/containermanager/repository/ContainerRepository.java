package com.sqljudge.containermanager.repository;

import com.sqljudge.containermanager.model.entity.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {
    Optional<Container> findByDockerContainerId(String dockerContainerId);
    List<Container> findByStatus(Container.ContainerStatus status);
    List<Container> findAllByStatus(Container.ContainerStatus status);

    @Query("SELECT COUNT(c) FROM Container c WHERE c.status = ?1")
    long countByStatus(Container.ContainerStatus status);

    @Query("SELECT c FROM Container c WHERE c.status = 'AVAILABLE' AND c.useCount < c.maxUses")
    List<Container> findAvailableContainers();

    @Query(value = "SELECT * FROM containers c WHERE c.status = 'AVAILABLE' AND c.use_count < c.max_uses ORDER BY c.id ASC LIMIT 1", nativeQuery = true)
    Optional<Container> findAvailableContainer();

    @Query("SELECT c FROM Container c WHERE c.useCount >= c.maxUses OR c.status = 'ERROR'")
    List<Container> findContainersNeedingReplacement();
}
