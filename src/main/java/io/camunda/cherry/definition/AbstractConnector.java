/* ******************************************************************** */
/*                                                                      */
/*  Abstract Connector                                                     */
/*                                                                      */
/*  To be manage by Cherry, a worker must extend this class            */
/*  It contains the basic information required by Cherry                */
/*   - define the type, and Input/Output/Errors                         */
/*   - be able to give description, logo, name                          */
/*  and the contract implementation on parameters                       */
/* ******************************************************************** */
package io.camunda.cherry.definition;

import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractConnector extends AbstractRunner implements OutboundConnectorFunction {
  Logger logger = LoggerFactory.getLogger(AbstractConnector.class.getName());

  /**
   * Theses class describe the Input and Output class. The class contains a getListParameters()
   * function
   */
  private AbstractConnectorInput abstractConnectorInput;

  private AbstractConnectorOutput abstractConnectorOutput;

  /**
   * When an existing Connector is embedded, this class saved the orginal Input and Output class.
   * They don't contains any getListParameters() function
   */
  private Class connectorInputClass;

  private Class connectorOutputClass;

  /**
   * Classical connector, all information are provided
   *
   * @param type           type of the connector
   * @param listInput      list of Input parameters for the worker
   * @param listOutput     list of Output parameters for the worker
   * @param listBpmnErrors list of potential BPMN ControllerPage the worker can generate
   */
  public AbstractConnector(String type,
                           List<RunnerParameter> listInput,
                           Class<?> connectorInputClass,
                           List<RunnerParameter> listOutput,
                           Class<?> connectorOutputClass,
                           List<BpmnError> listBpmnErrors) {
    super(type, listInput, listOutput, listBpmnErrors);
    this.connectorInputClass = connectorInputClass;
    this.connectorOutputClass = connectorOutputClass;
  }

  /**
   * Introspection constructor. Input Class and Output class are provided, and introspected to
   * determine Inputs and Outputs
   *
   * @param type                 type of the connector
   * @param connectorInputClass  Input class, where InputParameters are defined
   * @param connectorOutputClass Output class, where OutputParameters are defined
   * @param listBpmnErrors       list of BPMN error that the connector can throw
   */
  protected AbstractConnector(String type,
                              Class<?> connectorInputClass,
                              Class<?> connectorOutputClass,
                              List<BpmnError> listBpmnErrors) {
    super(type, Collections.emptyList(), Collections.emptyList(), listBpmnErrors);

    // Create class ConnectorInput/ConnectorOutput to get parameters
    try {
      for (Constructor constructor : connectorInputClass.getConstructors()) {
        if (constructor.getParameterCount() == 0) {
          Object inputClass = connectorInputClass.getConstructors()[0].newInstance();
          if (inputClass instanceof AbstractConnectorInput abstractConnectorInput) {
            this.abstractConnectorInput = abstractConnectorInput;
            setListInput(abstractConnectorInput.getInputParameters());
          }
        }
      }
      // the input is not an AbstractConnectorInput, but a simple object: reference it as it, but
      // there is no input then.
      if (this.abstractConnectorInput == null)
        this.connectorInputClass = connectorInputClass;

    } catch (Exception e) {
      logger.error("AbstractConnector: can't create ConnectorInput to get listOfParameters " + e);
    }
    if (connectorOutputClass != null) {
      try {

        for (Constructor constructor : connectorOutputClass.getConstructors()) {
          if (constructor.getParameterCount() == 0) {
            Object outputClass = constructor.newInstance();
            if (outputClass instanceof AbstractConnectorOutput abstractConnectorOutput) {
              this.abstractConnectorOutput = abstractConnectorOutput;
              setListOutput(abstractConnectorOutput.getOutputParameters());
            }
          }
        }
        // the input is not an AbstractConnectorInput, but a simple object: reference it as it, but
        // there is no ouput then.
        if (this.abstractConnectorOutput == null)
          this.connectorOutputClass = connectorInputClass;

      } catch (Exception e) {
        logger.error("AbstractConnector: can't create ConnectorOutput to get list OfParameters" + e);
      }
    }
  }

  @Override
  public ValidationResult checkValidDefinition() {
    ValidationResult validationResult = super.checkValidDefinition();

    // control at this level
    // According to the constructor, we get directly the list or we have to ask for it
    if (abstractConnectorInput != null) {
      AbstractConnectorInput.InputParametersInfo parameterInfo = abstractConnectorInput.getInputParametersInfo();
      if (parameterInfo != null && !parameterInfo.listRunners().isEmpty() && parameterInfo.inputClass() != null)
        validationResult.listOfErrors()
            .addAll(confrontParameterWithClass(parameterInfo.inputClass(), parameterInfo.listRunners(), "INPUT"));
    }
    if (connectorInputClass != null) {
      validationResult.listOfErrors().addAll(confrontParameterWithClass(connectorInputClass, getListInput(), "INPUT"));
    }

    if (abstractConnectorOutput != null) {
      AbstractConnectorOutput.OutputParametersInfo parameterInfo = abstractConnectorOutput.getOutputParametersInfo();
      if (parameterInfo != null && !parameterInfo.listRunners().isEmpty() && parameterInfo.outputClass() != null)
        validationResult.listOfErrors()
            .addAll(confrontParameterWithClass(parameterInfo.outputClass(), parameterInfo.listRunners(), "OUTPUT"));
    }
    if (connectorInputClass != null) {
      validationResult.listOfErrors()
          .addAll(confrontParameterWithClass(connectorOutputClass, getListOutput(), "OUTPUT"));
    }

    // Check if the output is correctly define
    Class<?> classConnectorOutput = getConnectorOutputClass();
    Class<?> classConnectorOutputVerification =
        classConnectorOutput == null ? abstractConnectorOutput.getClass() : classConnectorOutput;
    // there is no OutputClass? So return NoError
    if (classConnectorOutputVerification == null)
      return validationResult;

    // ATTENTION, the output must start with a lower case. But this is not mandatory, the connector
    // can return the object, not each method
    if (getListOutput().size() == 0 && classConnectorOutputVerification != null) {
      validationResult.listOfWarnings()
          .add("No Output are defined, but one object is returning by the connector - defined the output");
    }
    if (getListOutput().size() == 1 && getListOutput().get(0).getClass().equals(Object.class)) {
      // this is acceptable, the connector return an Object
    } else {
      for (RunnerParameter runnerParameter : getListOutput()) {
        // do not generate a property for accessAllVariables
        if (runnerParameter.isAccessAllVariables())
          continue;
        if (runnerParameter.getName().isEmpty()) {
          validationResult.listOfErrors().add("One parameters does not have a name");
          continue;
        }
        String firstLetter = runnerParameter.getName().substring(0, 1);

        if (!firstLetter.toLowerCase().equals(firstLetter)) {
          validationResult.listOfErrors().add("The first letter must be in Lower case");
          continue;
        }

        // check if a method get<runnerParameter.getName()> exist
        Method m = null;
        try {
          m = classConnectorOutputVerification.getMethod("get" + runnerParameter.getName(), null);
        } catch (NoSuchMethodException e) {
          validationResult.listOfErrors()
              .add("A method [get" + runnerParameter.getName() + "()] must exist in class "
                  + classConnectorOutputVerification.getName());
          continue;
        }
      }
    }
    return validationResult;
  }

  public AbstractConnectorInput getAbstractConnectorInput() {
    return abstractConnectorInput;
  }

  public AbstractConnectorOutput getAbstractConnectorOutput() {
    return abstractConnectorOutput;
  }

  public Class getConnectorInputClass() {
    return connectorInputClass;
  }

  public Class getConnectorOutputClass() {
    return connectorOutputClass;
  }

  /**
   * Confront a list of RunnerParameter with a class.
   *
   * @param clazz      class to confront
   * @param parameters list of Runner.
   * @return empty is every thing is OK, else an analysis
   */
  private List<String> confrontParameterWithClass(Class clazz, List<RunnerParameter> parameters, String label) {
    List<String> listOfErrors = new ArrayList<>();

    Field[] fields = clazz.getDeclaredFields();
    // All fields are part of parameters?
    for (Field field : fields) {
      // ignore static field
      if (Modifier.isStatic(field.getModifiers()))
        continue;
      long number = parameters.stream().filter(t -> t.getName().equals(field.getName())).count();
      if (number != 1)
        listOfErrors.add(label + ":Class Field[" + field.getName() + "] is not part of parameters");
    }

    // All parameters must be part of the fields
    for (RunnerParameter parameter : parameters) {
      if (parameter.getName().equals("*"))
        continue;
      long number = Stream.of(fields)
          .filter(field -> !Modifier.isStatic(field.getModifiers()))
          .filter(field -> field.getName().equals(parameter.getName()))
          .count();
      if (number != 1)
        listOfErrors.add(label + ":Parameter[" + parameter.getName() + "] is not part of fields in the class");
    }

    return listOfErrors;
  }

  public boolean isWorker() {
    return false;
  }

  public boolean isConnector() {
    return true;
  }
}
