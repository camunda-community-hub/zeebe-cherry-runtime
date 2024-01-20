package io.camunda.cherry.definition.connector;

import io.camunda.cherry.definition.RunnerParameter;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SdkRunnerCherryConnector extends SdkRunnerConnector {

  private final OutboundConnectorFunction outboundConnectorFunction;

  Logger logger = LoggerFactory.getLogger(SdkRunnerCherryConnector.class.getName());

  public SdkRunnerCherryConnector(OutboundConnectorFunction outboundConnectorFunction) {

    super(outboundConnectorFunction); // listBpmnErrors);
    this.outboundConnectorFunction = outboundConnectorFunction;
  }

  /**
   * Detect if the class is a Cherry Runner Connector. This class has declared some additional method: getType()
   * Some optional method may exist: getLogo(), getCollectionName(), getDescription()
   * The Input class has a getInputParameters() methods, and output a getOutputParameters()
   *
   * @param connectorClass class to verify
   * @return true if the class is a Cherry connector
   */
  public static boolean isRunnerCherryConnector(Class<?> connectorClass) {
    try {
      Method method = connectorClass.getMethod("getLogo");
      return true;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  @Override
  public OutboundConnectorFunction getTransportedConnector() {
    return outboundConnectorFunction;
  }

  /**
   * The type is known after, in the annotation for example
   *
   * @param type type to set
   */
  @Override
  public void setType(String type) {
    super.setType(type);
  }

  @Override
  public String getLogo() {
    return callMethodString("getLogo");
  }

  @Override
  public String getDescription() {
    return callMethodString("getDescription");
  }

  @Override
  public String getCollectionName() {
    return callMethodString("getCollectionName");
  }

  public Map<String, String> getBpmnErrors() {
    Object value = callMethod(outboundConnectorFunction, "getBpmnErrors", Map.class);
    return (Map<String, String>) value;
  }

  @Override
  public List<RunnerParameter> getListInput() {
    Class classInput = getInputParameterClass();
    if (classInput == null)
      return super.getListInput();
    try {
      // Create an instance of the dynamically determined class using the constructor
      Constructor<?> constructor = classInput.getDeclaredConstructor();
      Object objectInput = constructor.newInstance();

      Object listInputParameters = callMethod(objectInput, "getInputParameters", List.class);
      if (listInputParameters instanceof List listInputs)
        return transformList(listInputs, "ConnectorName[" + this.getName() + "]");
      else if (listInputParameters != null)
        logger.error("Error during getListInput(): on ConnectorName[{}] expect List get {}", this.getName(),
            listInputParameters.getClass().getName());
      return Collections.emptyList();

    } catch (Exception e) {
      logger.error("Error during getListInput(): on ConnectorName[{}] result : {}", this.getName(), e);
      return Collections.emptyList();
    }
  }

  @Override
  public List<RunnerParameter> getListOutput() {
    Class classOutput = getOutputParameterClass();
    if (classOutput == null)
      return Collections.emptyList();

    try {
      // Create an instance of the dynamically determined class using the constructor
      Constructor<?> constructor = classOutput.getDeclaredConstructor();
      Object objectInput = constructor.newInstance();

      Object listOutputParameter = callMethod(objectInput, "getOutputParameters", List.class);
      if (listOutputParameter instanceof List listInputs)
        return transformList(listInputs, "ConnectorName[" + this.getName() + "]");
      else if (listOutputParameter != null)
        logger.error("Error during getListInput(): on ConnectorName[{}] expect List get {}", this.getName(),
            listOutputParameter.getClass().getName());
      return Collections.emptyList();

    } catch (Exception e) {
      logger.error("Error during getListInput(): on ConnectorName[{}] result : {}", this.getName(), e);
      return Collections.emptyList();
    }
  }

  public Class getInputParameterClass() {
    Object value = callMethod(outboundConnectorFunction, "getInputParameterClass", Class.class);
    return (Class) value;
  }

  public Class getOutputParameterClass() {
    Object value = callMethod(outboundConnectorFunction, "getOutputParameterClass", Class.class);
    return (Class) value;
  }

  private String callMethodString(String name) {
    return (String) callMethod(outboundConnectorFunction, name, String.class);
  }

  private Object callMethod(Object caller, String name, Class valueClass) {
    Method method = null;
    try {
      method = caller.getClass().getMethod(name);
      // the method does not exist, return value null
      if (method == null) {
        return null;
      }
      Object value = method.invoke(caller);
      if (value != null && !valueClass.isInstance(value)) {
        logger.error("Error during {}(): on [{}] result class{} is not the expected result {}", name, this.getName(),
            value.getClass().getName(), valueClass.getName());
        return null;
      }
      return value;
    } catch (NoSuchMethodException ne) {
      // do nothing, no log please
      return null;
    } catch (Exception e) {
      logger.error("Error during {}(): on [{}] {}", name, this.getName(), e.toString());
      return null;
    }
  }

  private List<RunnerParameter> transformList(List<Object> listInputsParameter, String contextInfo) {
    List<RunnerParameter> listRunnersParameters = new ArrayList<>();
    for (Object input : listInputsParameter) {
      if (input instanceof Map inputMap) {
        RunnerParameter parameter = new RunnerParameter();
        parameter.name = getStringFromMap(inputMap, "name", contextInfo);
        parameter.label = getStringFromMap(inputMap, "label", contextInfo);
        parameter.clazz = (Class) inputMap.get("class");
        parameter.level = RunnerParameter.Level.valueOf(getStringFromMap(inputMap, "level", contextInfo));
        parameter.explanation = getStringFromMap(inputMap, "explanation", contextInfo);
        parameter.defaultValue = inputMap.get("defaultValue");
        parameter.conditionProperty = getStringFromMap(inputMap, "conditionProperty", contextInfo);

        parameter.gsonTemplate = getStringFromMap(inputMap, "gsonTemplate", contextInfo);
        parameter.conditionOneOf = (List<String>) inputMap.get("conditionOneOf");

        List<Object> workerParameterChoiceList = (List) inputMap.get("workerParameterChoiceList");
        if (workerParameterChoiceList != null) {
          List<RunnerParameter.WorkerParameterChoice> workerParameterList = new ArrayList<>();
          for (Object workerParameter : workerParameterChoiceList) {
            if (workerParameter instanceof Map workerParameterMap) {
              String code = getStringFromMap(workerParameterMap, "code", contextInfo + ".workerParameterChoiceList");
              String displayName = getStringFromMap(workerParameterMap, "displayName",
                  contextInfo + ".workerParameterChoiceList");
              RunnerParameter.WorkerParameterChoice workerParameterChoice = new RunnerParameter.WorkerParameterChoice(
                  code, displayName);
              workerParameterList.add(workerParameterChoice);
            } else {
              logger.error("Error during transformList.workerParameterChoiceList{} : List Of Map expected, get {}",
                  workerParameter == null ? "null" : workerParameter.getClass().getName());
            }
          }
        } // end workerParameterChoiceList != null
        parameter.visibleInTemplate = Boolean.TRUE.equals(inputMap.get("visibleInTemplate"));
        listRunnersParameters.add(parameter);
        // public RunnerParameter.Group group;
      } else // input is not a Map
        logger.error("Error during transformList {} : List Of Map expected, get {}", contextInfo,
            input == null ? "null" : input.getClass().getName());
    }
    return listRunnersParameters;
  }

  public String getStringFromMap(Map<?, ?> map, String attributName, String contextInfo) {
    Object value = map.getOrDefault(attributName, null);
    if (value == null)
      return null;
    if (value instanceof String)
      return (String) value;

    logger.error("Error during getString in {}, attribut {} : String expected get {}", contextInfo, attributName,
        value.getClass().getName());
    return null;
  }
}
