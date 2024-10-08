/* ******************************************************************** */
/*                                                                      */
/*  RunnerInformation                                                   */
/*                                                                      */
/*  Collect worker information from a worker.                           */
/*  This class works as a facade. It's easy then to get the JSON        */
/*  from this object                                                    */
/* ******************************************************************** */
package io.camunda.cherry.admin;

import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.AbstractWorker;
import io.camunda.cherry.definition.BpmnError;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.cherry.runtime.HistoryPerformance;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RunnerInformation {

  private AbstractRunner runner;

  /**
   * The runner has to status. When it asked to be stopped, the call waits until the jobs is done,
   * so technically we can consider it with this two status active or inactive
   */
  private boolean active;

  private boolean displayLogo = true;

  private HistoryFactory.Statistic statistic;

  private HistoryPerformance.Performance performance;

  /**
   * Keep the runner in the class. This class is a facade
   *
   * @param runner runner to get information from
   * @return runner information for a runner
   */
  public static RunnerInformation getRunnerInformation(AbstractRunner runner) {
    RunnerInformation workerInformation = new RunnerInformation();
    workerInformation.runner = runner;
    return workerInformation;
  }

  /**
   * Get all information on runner
   */
  public String getName() {
    return runner.getIdentification();
  }

  public String getLabel() {
    return runner.getLabel();
  }

  public String getDisplayLabel() {
    return runner.getDisplayLabel();
  }

  public String getType() {
    return runner.getType();
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public List<RunnerParameter> getListInput() {
    return runner.getListInput();
  }

  public List<RunnerParameter> getListOutput() {
    return runner.getListOutput();
  }

  public String getValidationErrorsMessage() {
    return String.join("; ", runner.checkValidDefinition().listOfErrors());
  }

  public String getValidationWarningsMessage() {
    return String.join("; ", runner.checkValidDefinition().listOfWarnings());
  }

  public List<BpmnError> getListBpmnErrors() {
    return runner.getListBpmnErrors();
  }

  public String getClassName() {
    return runner.getClass().getName();
  }

  public String getDescription() {
    return runner.getDescription();
  }

  public String getLogo() {
    return displayLogo ? runner.getLogo() : null;
  }

  public TYPE_RUNNER getTypeRunner() {
    return runner instanceof AbstractWorker ? TYPE_RUNNER.WORKER : TYPE_RUNNER.CONNECTOR;
  }

  public void setDisplayLogo(boolean displayLogo) {
    this.displayLogo = displayLogo;
  }

  /**
   * If the runner has definition error, is will not be possible to start it
   *
   * @return the list of errors
   */
  public String getDefinitionErrors() {
    return String.join(", ", runner.checkValidDefinition().listOfErrors());
  }

  public HistoryFactory.Statistic getStatistic() {
    return statistic;
  }

  public void setStatistic(HistoryFactory.Statistic statistic) {
    this.statistic = statistic;
  }

  public HistoryPerformance.Performance getPerformance() {
    return performance;
  }

  public void setPerformance(HistoryPerformance.Performance performance) {
    this.performance = performance;
  }

  public enum TYPE_RUNNER {
    WORKER, CONNECTOR
  }
}
