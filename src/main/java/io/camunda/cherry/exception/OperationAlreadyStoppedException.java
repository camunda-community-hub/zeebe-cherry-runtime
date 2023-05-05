package io.camunda.cherry.exception;

public class OperationAlreadyStoppedException extends OperationException {
  public OperationAlreadyStoppedException() {
    super("RUNNER_ALREADY_STOPPED", "Runner already stopped");
  }
}
