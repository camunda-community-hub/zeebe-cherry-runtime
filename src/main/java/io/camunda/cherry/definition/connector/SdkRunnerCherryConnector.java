package io.camunda.cherry.definition.connector;

import io.camunda.cherry.definition.BpmnError;
import io.camunda.connector.cherrytemplate.RunnerParameter;
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

  /**
   * Use the CherryInput constant *
   * public static final String PARAMETER_MAP_NAME = "name";
   * public static final String PARAMETER_MAP_LABEL = "label";
   * public static final String PARAMETER_MAP_CLASS = "class";
   * public static final String PARAMETER_MAP_LEVEL = "level";
   * public static final String PARAMETER_MAP_EXPLANATION = "explanation";
   * public static final String PARAMETER_MAP_DEFAULT_VALUE = "defaultValue";
   * public static final String PARAMETER_MAP_CONDITION_PROPERTY = "conditionProperty";
   * public static final String PARAMETER_MAP_GSON_TEMPLATE = "gsonTemplate";
   * public static final String PARAMETER_MAP_CONDITION_ONE_OF = "conditionOneOf";
   * public static final String PARAMETER_MAP_CHOICE_LIST = "choiceList";
   * public static final String PARAMETER_MAP_CHOICE_LIST_CODE = "code";
   * public static final String PARAMETER_MAP_CHOICE_LIST_DISPLAY_NAME = "displayName";
   * public static final String PARAMETER_MAP_VISIBLE_IN_TEMPLATE = "visibleInTemplate";
   */
  private final OutboundConnectorFunction outboundConnectorFunction;

  Logger logger = LoggerFactory.getLogger(SdkRunnerCherryConnector.class.getName());

  public SdkRunnerCherryConnector(OutboundConnectorFunction outboundConnectorFunction) {

    super(outboundConnectorFunction);
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
      return method!=null;
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

  @Override
  public List<String> getAppliesTo() {
    try {
      return (List<String>) callMethod(outboundConnectorFunction, "getAppliesTo", List.class);
    }
    catch(Exception e) {
      logger.error("method[List<String> getAppliesTo()]  is not correctly defined {}",e);
      return Collections.emptyList();
      }
  }

  @Override
  public List<BpmnError> getListBpmnErrors() {
    Object value = callMethod(outboundConnectorFunction, "getListBpmnErrors", Map.class);
    if (value instanceof Map<?, ?> listErrorsMap) {
      List<BpmnError> listErrors = new ArrayList<>();
      for (Map.Entry<?, ?> entry : listErrorsMap.entrySet()) {
        if (entry.getKey() != null && entry.getValue() != null) {
          listErrors.add(new BpmnError(entry.getKey().toString(), entry.getValue().toString()));
        } else {
          logger.error("getListBpmnErrors: Must be Map<String,String> with String (no null value)");

        }
      }
      return listErrors;
    }
    logger.error("getListBpmnErrors does not return a Map<String,String> as expected ");
    return Collections.emptyList();
  }

  @Override
  public List<RunnerParameter> getListInput() {
    Class<?> classInput = getInputParameterClass();
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
    Class<?> classOutput = getOutputParameterClass();
    if (classOutput == null)
      return Collections.emptyList();

    try {
      // Create an instance of the dynamically determined class using the constructor
      Constructor<?> constructor = classOutput.getDeclaredConstructor();
      // Force the constructor, in case the developper forget to set it public
      constructor.setAccessible(true);
      Object objectInput = constructor.newInstance();

      Object listOutputParameter = callMethod(objectInput, "getOutputParameters", List.class);
      if (listOutputParameter instanceof List listOutput)
        return transformList(listOutput, "ConnectorName[" + this.getName() + "]");
      else if (listOutputParameter != null)
        logger.error("Error during getListInput(): on ConnectorName[{}] expect List get {}", this.getName(),
            listOutputParameter.getClass().getName());
      return Collections.emptyList();

    } catch (Exception e) {
      logger.error("Error during getListInput(): on ConnectorName[{}] result : {}", this.getName(), e);
      return Collections.emptyList();
    }
  }

  public Class<?> getInputParameterClass() {
    Object value = callMethod(outboundConnectorFunction, "getInputParameterClass", Class.class);
    return (Class) value;
  }

  public Class<?> getOutputParameterClass() {
    Object value = callMethod(outboundConnectorFunction, "getOutputParameterClass", Class.class);
    return (Class) value;
  }

  private String callMethodString(String name) {
    return (String) callMethod(outboundConnectorFunction, name, String.class);
  }

  private Object callMethod(Object caller, String name, Class<?> valueClass) {
    Method method = null;
    try {
      method = caller.getClass().getMethod(name);
      // if the developer forget to put it public
      // the method does not exist, return value null
      if (method == null) {
        return null;
      }
      method.setAccessible(true);
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
        try {
          RunnerParameter runner = RunnerParameter.fromMap(inputMap, contextInfo);
          listRunnersParameters.add(runner);
        } catch(Exception e) {
          logger.error("Can't convert RunnerParameter from map {} : {}", inputMap, e);
        }
      } else // input is not a Map
        logger.error("Error during transformList {} : List Of Map expected, get {}", contextInfo,
            input == null ? "null" : input.getClass().getName());
    }
    return listRunnersParameters;
  }

}
