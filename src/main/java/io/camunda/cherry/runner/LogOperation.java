/* ******************************************************************** */
/*                                                                      */
/*  LogOperation                                                          */
/*                                                                      */
/*  Every operation is logged here.                                     */
/* ******************************************************************** */
package io.camunda.cherry.runner;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.repository.OperationRepository;
import io.camunda.cherry.definition.AbstractRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class LogOperation {

  Logger logger = LoggerFactory.getLogger(LogOperation.class.getName());

  @Autowired
  OperationRepository operationRepository;

  /**
   * log an operation
   *
   * @param operation type of operation
   * @param message   message
   */
  public void log(OperationEntity.Operation operation, String message) {
    logger.info("Operation {} [{}]", operation.toString(), message);
    OperationEntity operationEntity = new OperationEntity();
    operationEntity.operation = operation;
    operationEntity.executionTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    operationEntity.hostName = getHostName();
    operationEntity.message = message;
    saveOperationEntity(operationEntity);
  }

  /**
   * Specify an operation on a specific worker
   *
   * @param operation type of operation
   * @param runner    specific runner
   * @param message   message to log
   */
  public void log(OperationEntity.Operation operation, AbstractRunner runner, String message) {
    logger.info("Operation {} on Runner[{}] {}", operation.toString(), runner.getName(), message);
    OperationEntity operationEntity = new OperationEntity();
    operationEntity.operation = operation;
    operationEntity.executionTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    operationEntity.runnerType = runner.getType();
    operationEntity.hostName = getHostName();
    operationEntity.message = message;
    saveOperationEntity(operationEntity);
  }

  /**
   * OperationLog an error
   *
   * @param runner  an error on a specific runner
   * @param message contextual message (what operation was performed)
   * @param e       exception during the error
   */
  public void logException(AbstractRunner runner, String message, Exception e) {
    logger.error("Exception Runner[{}] {} {}", runner.getName(), message, e.getMessage());
    OperationEntity operationEntity = new OperationEntity();
    operationEntity.operation = OperationEntity.Operation.ERROR;
    operationEntity.executionTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    operationEntity.runnerType = runner.getType();
    operationEntity.hostName = getHostName();
    operationEntity.message = message + ": " + e.getMessage();
    saveOperationEntity(operationEntity);
  }

  public void logError(String runnerType, String message, Error er) {
    logger.error("Exception Runner[{}] {} {}", runnerType, message, er.getMessage());
    OperationEntity operationEntity = new OperationEntity();
    operationEntity.operation = OperationEntity.Operation.ERROR;
    operationEntity.executionTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    operationEntity.runnerType = runnerType;
    operationEntity.hostName = getHostName();
    operationEntity.message = message + ": " + er.getMessage();
    saveOperationEntity(operationEntity);
  }

  public void logException(String runnerType, String message, Exception ex) {
    logger.error("Exception Runner[{}] {} {}", runnerType, message, ex.getMessage());
    OperationEntity operationEntity = new OperationEntity();
    operationEntity.executionTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    operationEntity.operation = OperationEntity.Operation.ERROR;
    operationEntity.runnerType = runnerType;
    operationEntity.hostName = getHostName();
    operationEntity.message = message + ": " + ex.getMessage();
    saveOperationEntity(operationEntity);
  }

  /**
   * OperationLog an error
   *
   * @param message contextual message (what operation was performed)
   * @param e       exception
   */
  public void logError(String message, Exception e) {
    logger.error("Exception {} {}", message, e);
    OperationEntity operationEntity = new OperationEntity();
    operationEntity.executionTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    operationEntity.operation = OperationEntity.Operation.ERROR;
    operationEntity.hostName = getHostName();
    operationEntity.message = message + ": " + e.getMessage();
    saveOperationEntity(operationEntity);
  }

  /**
   * OperationLog an error
   *
   * @param message contextual message (what operation was performed)
   */
  public void logError(String message) {
    logger.error("Error {}", message);
    OperationEntity operationEntity = new OperationEntity();
    operationEntity.executionTime = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    operationEntity.operation = OperationEntity.Operation.ERROR;
    operationEntity.hostName = getHostName();
    operationEntity.message = message;
    saveOperationEntity(operationEntity);
  }

  private String getServerIdentification() {
    return getHostName();
  }

  private String getHostName() {
    try {
      InetAddress ipAddress = InetAddress.getLocalHost();

      return ipAddress.getHostName();
    } catch (Exception e) {
      return "CherryHostName";
    }
  }

  private void saveOperationEntity(OperationEntity operationEntity) {
    try {
      operationRepository.save(operationEntity);
    } catch (Exception e) {
      logger.error("Can't save OperationEntity [{}]", operationEntity);
    }
  }
}
