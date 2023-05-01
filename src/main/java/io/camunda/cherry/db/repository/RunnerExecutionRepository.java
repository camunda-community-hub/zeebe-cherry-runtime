package io.camunda.cherry.db.repository;

import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.definition.AbstractRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface RunnerExecutionRepository extends JpaRepository<RunnerExecutionEntity, Long> {

  @Query("select runnerexecution from RunnerExecutionEntity runnerexecution"
      + " where runnerexecution.executionTime >= :dateToSearch ")
  RunnerExecutionEntity findLastRecord(@Param("dateToSearch") Instant dateToSearch);

  @Query(
      "select runnerexecution.status as status, count(runnerexecution) as number from RunnerExecutionEntity runnerexecution"
          + " where runnerexecution.executionTime >= :dateToSearch " + " and runnerexecution.runnerType = :runnerType"
          + " group by runnerexecution.status")
  List<Map<String, Object>> selectStatusStats(@Param("runnerType") String runnerType,
                                              @Param("dateToSearch") LocalDateTime dateToSearch);

  @Query(value = "select runnerexecution from RunnerExecutionEntity runnerexecution"
      + " where runnerexecution.executionTime >= :dateToSearch " + " and runnerexecution.runnerType = :runnerType"
      + " order by runnerexecution.executionTime desc")
  List<RunnerExecutionEntity> selectRunnerRecords(@Param("runnerType") String runnerType,
                                                  @Param("dateToSearch") LocalDateTime dateToSearch,
                                                  Pageable pageable);

  @Query(value = "select runnerexecution from RunnerExecutionEntity runnerexecution"
      + " where runnerexecution.executionTime >= :dateToSearch " + " and runnerexecution.runnerType = :runnerType"
      + " and runnerexecution.status in (:listStates)" + " order by runnerexecution.executionTime desc")
  List<RunnerExecutionEntity> selectRunnerRecordsByStates(@Param("runnerType") String runnerType,
                                                          @Param("dateToSearch") LocalDateTime dateToSearch,
                                                          @Param("listStates") List<AbstractRunner.ExecutionStatusEnum> listStates,
                                                          Pageable pageable);

}
