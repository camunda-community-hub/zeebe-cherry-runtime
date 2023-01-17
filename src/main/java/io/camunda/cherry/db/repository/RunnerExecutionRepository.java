package io.camunda.cherry.db.repository;

import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface RunnerExecutionRepository extends JpaRepository<RunnerExecutionEntity, Long> {

  @Query("select runnerexecution from RunnerExecutionEntity runnerexecution"
      + " where runnerexecution.executionTime >= :dateToSearch ")
  public RunnerExecutionEntity findLastRecord(@Param("dateToSearch") Instant dateToSearch);

  @Query("select runnerexecution.status, count(runnerexecution) as number from RunnerExecutionEntity runnerexecution"
      + " where runnerexecution.executionTime >= :dateToSearch " + " group by runnerexecution.status")
  public List<Map<String, Object>> selectStatusStats(@Param("dateToSearch") Instant dateToSearch);

  @Query("select runnerexecution from RunnerExecutionEntity runnerexecution"
      + " where runnerexecution.executionTime >= :dateToSearch " + " and runnerexecution.runnerName = :runnerName")
  public List<RunnerExecutionEntity> selectRunnerRecords(@Param("dateToSearch") Instant dateToSearch,
                                                         @Param("runnerName") String runnerName);

}
