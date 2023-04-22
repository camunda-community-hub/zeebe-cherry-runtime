package io.camunda.cherry.db.repository;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OperationRepository extends JpaRepository<OperationEntity, Long> {

  @Query("select operationEntity from OperationEntity operationEntity"
      + " where operationEntity.executionTime >= :dateToSearch"
      + " and operationEntity.runnerType = :runnerType ")
  List<RunnerExecutionEntity> selectByRunnerType(@Param("runnerType") String runnerType,
                                                 @Param("dateToSearch") Instant dateToSearch);

}
