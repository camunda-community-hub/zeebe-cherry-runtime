package io.camunda.cherry.exception;

public class OperationTooManyRunnersException extends OperationException {

  public OperationTooManyRunnersException(String information) {
    super("TooManyRunners", "Too many runners "+information);
  }
}