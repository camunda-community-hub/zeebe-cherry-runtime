package io.camunda.cherry.definition;

import io.camunda.cherry.runtime.WatcherFactory;
import io.camunda.connector.api.error.ConnectorException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

@Component
public abstract class AbstractWatcher {

  private final List<RunnerParameter> listInput;
  private final List<RunnerParameter> listInputForOutputVariableName;
  private final List<RunnerParameter> listOutput;
  private final List<BpmnError> listBpmnErrors;
  private final String type;
  private final List<WatcherExecution> listWatcherExecutions = new ArrayList<>();
  WatcherFactory watcherFactory;
  private String name;

  /**
   * Constructor
   * OutputVariableName:
   * List Input information to produce the Output data.
   * Example, give "VARIABLE_FILENAME" mean
   * - VARIABLE_FILENAME must exist as a String in the configuration. Value if "DocumentApplicant"
   * - the watcher save the filename under the "DocumentApplicant" variable
   *
   * @param listInput                      list of Input parameters for the worker
   * @param listInputForOutputVariableName list the different variable name for output.
   * @param listOutput                     list of Output parameters for the worker
   */
  protected AbstractWatcher(String type,
                            List<RunnerParameter> listInput,
                            List<RunnerParameter> listInputForOutputVariableName,
                            List<RunnerParameter> listOutput,
                            List<BpmnError> listBpmnErrors) {
    this.type = type;
    this.listInput = listInput;
    this.listInputForOutputVariableName = listInputForOutputVariableName;
    this.listOutput = listOutput;
    this.listBpmnErrors = listBpmnErrors;
  }

  /**
   * Factory register itself explicitly, to avoid any dependency cycle
   *
   * @param watcherFactory watcher factory
   */
  public void setWatcherFactory(WatcherFactory watcherFactory) {
    this.watcherFactory = watcherFactory;
  }

  public List<RunnerParameter> getListInput() {
    return listInput;
  }

  public List<RunnerParameter> getListInputForOutputVariableName() {
    return listInputForOutputVariableName;
  }

  public List<RunnerParameter> getListOutput() {
    return listOutput;
  }

  public List<BpmnError> getListBpmnErrors() {
    return listBpmnErrors;
  }

  public String getType() {
    return type;
  }

  public abstract String getLabel();

  public abstract String getDescription();

  public abstract String getLogo();

  public abstract String getCollectionName();

  /**
   * A watcher has a name. Multiple watcher can be setup of a specific type of watcher, but name must be uniq in the factory
   *
   * @return name
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Validate input
   *
   * @return true if input are correct
   */
  public boolean validateInput() {
    return true;
  }

  /**
   * Time in millisecond between two turn of duty
   */
  public abstract long getDefaultSleepTimeMs();

  /**
   * Watcher specify if he needs a "tourOfDuty".
   * Some watcher need a TourOfDuty every XX ms to detect if new orders are coming.
   * For example, check if an email arrived.
   * <p>
   * Other watcher can do that detection/wake up by themselves, and then does not need a tourOfDuty. If something arrived,
   * they will manage that by themselves, wake up and do their job
   *
   * @return true if the watcher need a TourOfDuty
   */
  public abstract boolean needTourOfDuty();

  /**
   * Execute a tour of duty, and return any process instance to create. One process instance is created per Order
   * Do not call executeOrder() method, else each order will be executed twice.
   */

  public List<WatcherOrderInformation> tourOfDuty(WatcherExecution watcherExecution) {
    return Collections.emptyList();
  }

  /**
   * If the watcher manage by himself the detection, it can call this method to send new order to execute.
   * After each execution, the orderExecuted method is called.
   *
   * @param watcherExecution  watcherExecution
   * @param ordersInformation list of Order to execute
   */
  protected void executeOrders(WatcherExecution watcherExecution, List<WatcherOrderInformation> ordersInformation) {
    watcherFactory.executeOrders(watcherExecution, ordersInformation);
  }

  /**
   * This order Information was completed with success, a process instance is created
   *
   * @param watcherExecution   watcherExecution who created the order
   * @param orderInformation   order Information executed
   * @param operationInSuccess the order required was realised with success
   */
  public abstract void orderExecuted(WatcherExecution watcherExecution,
                                     WatcherOrderInformation orderInformation,
                                     boolean operationInSuccess);

  /**
   * Create a new WatcherExecution
   *
   * @param action     action to execution
   * @param inputs     Inputs for execution
   * @param parameters parameters for execution
   * @return a new WatcherExecution
   */
  public WatcherExecution createExecution(String name,
                                          WatcherAction action,
                                          Map<String, Object> inputs,
                                          Map<WatcherParameter, Object> parameters) {
    WatcherExecution watcherExecution = new WatcherExecution(this, name, action, inputs, parameters);
    listWatcherExecutions.add(watcherExecution);
    return watcherExecution;
  }

  public List<WatcherExecution> getListWatcherExecutions() {
    return listWatcherExecutions;
  }

  public void start(WatcherExecution watcherExecution) {
    try {
      watcherExecution.startupException = null;
      startup(watcherExecution);
      watcherExecution.running = true;
    } catch (Exception e) {
      watcherExecution.startupException = e;
    }
  }

  public void stop(WatcherExecution watcherExecution) {
    watcherExecution.running = false;
    try {
      watcherExecution.shutdownException = null;
      shutdown(watcherExecution);
    } catch (Exception e) {
      watcherExecution.shutdownException = e;
    }
  }

  public boolean isRunning(WatcherExecution watcherExecution) {
    return watcherExecution.running;
  }




  /* ******************************************************************** */
  /*                                                                      */
  /*  Stop/start                                                          */
  /*                                                                      */
  /* ******************************************************************** */

  public abstract void startup(WatcherExecution watcherExecution) throws ConnectorException;

  public abstract void shutdown(WatcherExecution watcherExecution) throws ConnectorException;

  /* ******************************************************************** */
  /*                                                                      */
  /*  Execution                                                           */
  /*  A Watcher class is the definition of the execution. One Watcher     */
  /*  can have multiple Execution
  /*                                                                      */
  /* ******************************************************************** */
  public enum WatcherParameter {ACTION, PROCESSNAME, PROCESSVERSION, PROCESSID, TASKNAME, TASKID, TIMEBETWEENDUTYINMS}

  public enum WatcherAction {CREATEPROCESSINSTANCEPERID}

  public class WatcherExecution {
    private final String name;
    private final AbstractWatcher watcher;

    private final WatcherAction action;

    private final Map<String, Object> mapInputs;

    private final Map<WatcherParameter, Object> mapParameters;
    /**
     * Follow the execution
     */
    protected Exception startupException;
    protected Exception shutdownException;
    protected boolean running = false;
    /**
     * Executor timer event (needTourOfDuty() == true)
     */
    private ScheduledExecutorService executor;

    protected WatcherExecution(AbstractWatcher watcher,
                               String name,
                               WatcherAction action,
                               Map<String, Object> inputs,
                               Map<WatcherParameter, Object> parameters) {
      this.watcher = watcher;
      this.name = name;
      this.action = action;
      this.mapInputs = inputs;
      this.mapParameters = parameters;
    }

    public AbstractWatcher getWatcher() {
      return watcher;
    }

    public String getName() {
      return name;
    }

    public WatcherAction getAction() {
      return action;
    }

    public Object getInput(String inputName) {
      return mapInputs.get(inputName);
    }

    /**
     * Create the order information from watcher parameters (action, other information)
     *
     * @return a new order information
     */
    public WatcherOrderInformation createOrderInformation() {
      WatcherOrderInformation orderInformation = new WatcherOrderInformation();
      orderInformation.orderAction = getAction();

      orderInformation.processId = (String) getParameter(AbstractWatcher.WatcherParameter.PROCESSID);
      return orderInformation;
    }

    /**
     * Populate an order information from an InputVariable with a value.
     * If the InputVariable is not given, nothing arrived
     *
     * @param orderInformation  order information to populate
     * @param inputVariableName InputVariable name. The value of this input is the name of the output
     * @param value             value to populated
     */
    public void populateOrderInformation(WatcherOrderInformation orderInformation,
                                         String inputVariableName,
                                         Object value) {
      if (mapInputs.get(inputVariableName) instanceof String inputVariableValue) {
        orderInformation.setVariable(inputVariableValue, value);
      }
    }

    /**
     * Return the variable name for an InputForOutput
     * Example, INPUT is "VARIABLE_FILENAME". Configuration give for this value "DocumentApplicant"
     * The method return "DocumentApplicant".
     *
     * @param inputVariable
     * @return
     */
    public String getOutputVariableName(String inputVariable) {
      return (mapInputs.get(inputVariable) instanceof String) ? (String) mapInputs.get(inputVariable) : null;
    }

    public Object getParameter(WatcherParameter parameterName) {
      return mapParameters.get(parameterName);
    }

    public ScheduledExecutorService getExecutor() {
      return executor;
    }

    public void setExecutor(ScheduledExecutorService executor) {
      this.executor = executor;
    }

  }
}
