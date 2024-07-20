package io.camunda.cherry.exception;

public class OperationCantStopRunnerException extends OperationException {

  public OperationCantStopRunnerException() {
    super("CantStopRunner", "Can't stop a worker in a limited time");
  }
}