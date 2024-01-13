package io.camunda.cherry.definition.connector;

import io.camunda.cherry.definition.AbstractRunner;

import java.lang.reflect.Method;
import java.util.Collections;

public class SdkRunnerWorker extends AbstractRunner {

  private final Object worker;
  public final  io.camunda.zeebe.spring.client.annotation.JobWorker annotation;
  public final Method handleMethod;

  public SdkRunnerWorker(Object worker, io.camunda.zeebe.spring.client.annotation.JobWorker annotation, Method handleMethod) {

    super("", // String type
        Collections.emptyList(), //  listInput
        Collections.emptyList(), //  listOutput
        Collections.emptyList()); // listBpmnErrors
    this.worker = worker;
    this.annotation = annotation;
    this.handleMethod = handleMethod;
  }

  public Object getTransportedObject() {
    return worker;
  }

  public Method getHandleMethod() {
    return handleMethod;
  }
  /**
   * Get the type from the annotation
   *
   */
  @Override
  public String getType() {
    return annotation.type();
  }

  /**
   * Return the name
   *
   * @return name
   */
  @Override
  public String getName() {
    return annotation.name();
  }

  /**
   * For the ID, we return the name of the transported object, not the RunnerConnector
   * @return the ID of the runner
   */
  @Override
  public String getId() {
    return getTransportedObject().getClass().getName();
  }

  public boolean isWorker() {
    return true;
  }

  public boolean isConnector() {
    return false;
  }

  public String toString() {
    return "SdkRunnerWorker:"+getName();
  }

  public Object getWorker() {
    return worker;
  }
}

