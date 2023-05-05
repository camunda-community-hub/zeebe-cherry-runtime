package io.camunda.cherry.db.repository;

import io.camunda.cherry.db.entity.OperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationRepository extends JpaRepository<OperationEntity, Long> {

  @Query("select operationEntity from OperationEntity operationEntity"
      + " where operationEntity.executionTime >= :dateAfter" + " and operationEntity.runnerType = :runnerType "
      + " order by operationEntity.executionTime desc")
  List<OperationEntity> selectByRunnerType(@Param("runnerType") String runnerType,
                                           @Param("dateAfter") LocalDateTime dateAfter);

  @Query("select operationEntity from OperationEntity operationEntity"
      + " where operationEntity.executionTime >= :dateAfter" + " order by operationEntity.executionTime desc")
  List<OperationEntity> selectAll(@Param("dateAfter") LocalDateTime dateAfter);

}
