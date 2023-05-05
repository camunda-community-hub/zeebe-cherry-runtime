/* ******************************************************************** */
/*                                                                      */
/*  OperationFactory                                                          */
/*                                                                      */
/*  Manage operations                                    */
/* ******************************************************************** */
package io.camunda.cherry.runtime;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.repository.OperationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationFactory {
  @Autowired
  OperationRepository operationRepository;

  /**
   * Select for a runner type all operations registered
   *
   * @param runnerType    runner type
   * @param dateNow       instant now for reference
   * @param dateThreshold date from which operations are searched
   * @return list of operations
   */
  public List<OperationEntity> getOperations(String runnerType, LocalDateTime dateNow, LocalDateTime dateThreshold) {
    return operationRepository.selectByRunnerType(runnerType, dateThreshold);
  }

  /**
   * Select for a runner type all operations registered
   *
   * @param dateNow       instant now for reference
   * @param dateThreshold date from which operations are searched
   * @return list of operations
   */
  public List<OperationEntity> getAllOperations(LocalDateTime dateNow, LocalDateTime dateThreshold) {
    return operationRepository.selectAll(dateThreshold);
  }
}
