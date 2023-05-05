package io.camunda.cherry.exception;

public class OperationAlreadyStartedException extends OperationException {

  public OperationAlreadyStartedException() {
    super("RUNNER_ALREADY_STARTED", "Runner already started");
  }
}

