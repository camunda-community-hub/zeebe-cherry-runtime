/* ******************************************************************** */
/*                                                                      */
/*  OperationFactory                                                          */
/*                                                                      */
/*  Manage operations                                    */
/* ******************************************************************** */
package io.camunda.cherry.runtime;

import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.db.repository.OperationRepository;
import io.camunda.cherry.db.repository.RunnerExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class OperationFactory {
  @Autowired
  OperationRepository operationRepository;

  /**
   * Select for a runner type all operations registered
   * @param runnerType runner type
   * @param instantNow instant now for reference
   * @param dateThreshold date from which operations are searched
   * @return
   */
  public List<RunnerExecutionEntity> getOperations(String runnerType,
                                                   Instant instantNow,
                                                   Instant dateThreshold) {
    return operationRepository.selectByRunnerType(runnerType, dateThreshold);
  }

}
