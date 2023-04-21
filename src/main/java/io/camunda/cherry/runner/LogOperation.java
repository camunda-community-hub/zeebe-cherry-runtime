/* ******************************************************************** */
/*                                                                      */
/*  LogOperation                                                          */
/*                                                                      */
/*  Every operation is logged here.                                     */
/* ******************************************************************** */
package io.camunda.cherry.runner;

import io.camunda.cherry.definition.AbstractRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogOperation {

  Logger logger = LoggerFactory.getLogger(LogOperation.class.getName());

  /**
   * log an operation
   *
   * @param typeoperation type of operation
   * @param message       message
   */
  public void log(TYPEOPERATION typeoperation, String message) {
    logger.info("Operation " + typeoperation.toString() + " [" + message);

  }

  /**
   * Specify an operation on a specific worker
   *
   * @param typeoperation type of operation
   * @param runner        specific runner
   * @param message       message to log
   */
  public void log(TYPEOPERATION typeoperation, AbstractRunner runner, String message) {
    logger.info("Operation " + typeoperation.toString() + " on Runner[" + runner.getName() + "] " + message);

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

  }

  public void logError(String runnerName, String message, Error er) {
    logger.error("Error " + message + " on Runner[" + runnerName + "] :" + er.getMessage());
  }

  public void logException(String runnerName, String message, Exception er) {
    logger.error("Error " + message + " on Runner[" + runnerName + "] :" + er.getMessage());

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

  public enum TYPEOPERATION {START, STOP, ERROR}
}
