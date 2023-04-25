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
    logger.info("Operation " + operation.toString() + " [" + message);
    OperationEntity operationEntity = new OperationEntity();
    operationEntity.operation = operation;
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
    logger.info("Operation " + operation.toString() + " on Runner[" + runner.getName() + "] " + message);
    OperationEntity operationEntity = new OperationEntity();
    operationEntity.operation = operation;
    operationEntity.runnerType = runner.getType();
    operationEntity.hostName = getHostName();
    operationEntity.message = message;
    saveOperationEntity(operationEntity);

  }

  /**
   * Log an error
   *
   * @param runner  an error on a specific runner
   * @param message contextual message (what operation was performed)
   * @param e       exception during the error
   */
  public void logException(AbstractRunner runner, String message, Exception e) {
    logger.error("Error " + message + " on Runner[" + runner.getName() + "] :" + e.getMessage());
    OperationEntity operationEntity = new OperationEntity();
    operationEntity.operation = OperationEntity.Operation.ERROR;
    operationEntity.runnerType = runner.getType();
    operationEntity.hostName = getHostName();
    operationEntity.message = message + ": " + e.getMessage();
    saveOperationEntity(operationEntity);

  }

  public void logError(String runnerType, String message, Error er) {
    logger.error("Error " + message + " on Runner[" + runnerType + "] :" + er.getMessage());
    OperationEntity operationEntity = new OperationEntity();
    operationEntity.operation = OperationEntity.Operation.ERROR;
    operationEntity.runnerType = runnerType;
    operationEntity.hostName = getHostName();
    operationEntity.message = message + ": " + er.getMessage();
    saveOperationEntity(operationEntity);
  }

  public void logException(String runnerType, String message, Exception ex) {
    logger.error("Error " + message + " on Runner[" + runnerType + "] :" + ex.getMessage());
    OperationEntity operationEntity = new OperationEntity();
    operationEntity.operation = OperationEntity.Operation.ERROR;
    operationEntity.runnerType = runnerType;
    operationEntity.hostName = getHostName();
    operationEntity.message = message + ": " + ex.getMessage();
    saveOperationEntity(operationEntity);

  }

  /**
   * Log an error
   *
   * @param message contextual message (what operation was performed)
   * @param e       exception
   */
  public void logError(String message, Exception e) {
    logger.error("Error " + message + " :" + e.getMessage());

  }

  private String getServerIdentification() {
    return "";
  }

  private String getHostName() {
    try {
      InetAddress IP = InetAddress.getLocalHost();

      return IP.getHostName();
    } catch (Exception e) {
      return "CherryHostName";
    }
  }

  private void saveOperationEntity(OperationEntity operationEntity) {
    try {
      operationRepository.save(operationEntity);
    } catch (Exception e) {
      logger.error("Can't save OperationEntity " + operationEntity);
    }
  }
}
