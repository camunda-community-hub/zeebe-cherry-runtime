/* ******************************************************************** */
/*                                                                      */
/*  AbstractRunner                                                    */
/*                                                                      */
/*  The Runner is the basis for Workers and Connector to operate in   */
/* the Cherry Framework. A Runner defined                            */
/*   - a type                                                           */
/*   - list of Input/Output/Error                                       */
/*   - optionally, description, logo                              */
/* The execution depends on the class: Worker or Connector              */
/*                                                                      */
/* ******************************************************************** */
package org.camunda.cherry.definition;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.camunda.cherry.definition.filevariable.FileVariable;
import org.camunda.cherry.definition.filevariable.FileVariableFactory;
import org.camunda.cherry.definition.filevariable.FileVariableReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class AbstractRunner {


    public final static String BPMNERROR_ACCESS_FILEVARIABLE = "ACCESS_FILEVARIABLE";
    public final static String BPMNERROR_SAVE_FILEVARIABLE = "SAVE_FILEVARIABLE";


    private final String type;
    private final List<BpmnError> listBpmnErrors;
    Logger loggerAbstract = LoggerFactory.getLogger(AbstractRunner.class.getName());
    /**
     * For the Connector class, this information is calculated after the constructor
     */
    private List<RunnerParameter> listInput;
    /**
     * For the Connector class, this information is calculated after the constructor
     */
    private List<RunnerParameter> listOutput;
    private String name;
    private String description;
    private String logo;
    private boolean isLogWorker = false;

    /**
     * Constructor
     *
     * @param type           name of the worker
     * @param listInput      list of Input parameters for the worker
     * @param listOutput     list of Output parameters for the worker
     * @param listBpmnErrors list of potential BPMN Error the worker can generate
     */

    protected AbstractRunner(String type,
                             List<RunnerParameter> listInput,
                             List<RunnerParameter> listOutput,
                             List<BpmnError> listBpmnErrors) {

        this.type = type;
        this.listInput = listInput;
        this.listOutput = listOutput;
        this.listBpmnErrors = listBpmnErrors;
    }


    /* -------------------------------------------------------- */
    /*                                                          */
    /*  getInput/setOutput                                      */
    /*                                                          */
    /* method to get variable value                             */
    /* -------------------------------------------------------- */

    /**
     * Return a value as Double
     *
     * @param parameterName name of the parameter
     * @param defaultValue  default value, if the variable does not exist or any error arrived (can't parse the value)
     * @param activatedJob  job passed to the worker
     * @return a Double value
     */
    public Double getInputDoubleValue(String parameterName, Double defaultValue, final ActivatedJob activatedJob) {
        if (!containsKeyInJob(parameterName, activatedJob))
            return (Double) getDefaultValue(parameterName, defaultValue);
        Object value = getValueFromJob(parameterName, activatedJob);
        if (value == null)
            return null;
        if (value instanceof Double valueDouble)
            return valueDouble;
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * return a value as a Map
     *
     * @param parameterName name of the parameter
     * @param defaultValue  default value, if the variable does not exist or any error arrived (can't parse the value)
     * @param activatedJob  job passed to the worker
     * @return a Map value
     */
    public Map getInputMapValue(String parameterName, Map defaultValue, final ActivatedJob activatedJob) {
        if (!containsKeyInJob(parameterName, activatedJob))
            return (Map) getDefaultValue(parameterName, defaultValue);

        Object value = getValueFromJob(parameterName, activatedJob);
        if (value == null)
            return null;
        if (value instanceof Map valueMap)
            return valueMap;

        return defaultValue;
    }

    /**
     * Return a value as Long
     *
     * @param parameterName name of the variable
     * @param defaultValue  default value, if the variable does not exist or any error arrived (can't parse the value)
     * @param activatedJob  job passed to the worker
     * @return a Double value
     */
    public Long getInputLongValue(String parameterName, Long defaultValue, final ActivatedJob activatedJob) {
        if (!containsKeyInJob(parameterName, activatedJob))
            return (Long) getDefaultValue(parameterName, defaultValue);
        Object value = getValueFromJob(parameterName, activatedJob);

        if (value == null)
            return null;
        if (value instanceof Long valueLong)
            return valueLong;
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Return a value as Duration. The value may be a Duration object, or a time in ms (LONG) or a ISO 8601 String representing the duration
     * https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-
     * https://fr.wikipedia.org/wiki/ISO_8601
     *
     * @param parameterName name of the variable
     * @param defaultValue  default value, if the variable does not exist or any error arrived (can't parse the value)
     * @param activatedJob  job passed to the worker
     * @return a Double value
     */
    public Duration getInputDurationValue(String parameterName, Duration defaultValue, final ActivatedJob activatedJob) {
        if (!containsKeyInJob(parameterName, activatedJob))
            return (Duration) getDefaultValue(parameterName, defaultValue);
        Object value = getValueFromJob(parameterName, activatedJob);
        if (value == null)
            return null;
        if (value instanceof Duration valueDuration)
            return valueDuration;
        if (value instanceof Long valueLong)
            return Duration.ofMillis(valueLong);
        try {
            return Duration.parse(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * get the FileVariable.
     * The file variable may be store in multiple storage. The format is given in the parameterStorageDefinition. This is a String which pilot
     * how to load the file. The value can be saved a JSON, or saved in a specific directory (then the value is an ID)
     *
     * @param parameterName name where the value is stored
     * @param activatedJob  job passed to the worker
     * @return a FileVariable
     */
    public FileVariable getFileVariableValue(String parameterName, final ActivatedJob activatedJob) throws ZeebeBpmnError {
        if (!containsKeyInJob(parameterName, activatedJob))
            return null;
        Object fileVariableReferenceValue = getValueFromJob(parameterName, activatedJob);
        try {
            FileVariableReference fileVariableReference = FileVariableReference.fromJson(fileVariableReferenceValue.toString());

            if (fileVariableReference == null)
                return null;

            return FileVariableFactory.getInstance().getFileVariable(fileVariableReference);
        } catch (Exception e) {
            throw new ZeebeBpmnError(BPMNERROR_ACCESS_FILEVARIABLE, "Worker [" + getName() + "] error during access fileVariableReference[" + fileVariableReferenceValue + "] :" + e);
        }
    }

    /**
     * return a variable value
     *
     * @param parameterName name of the input value
     * @param defaultValue  if the input does not exist, this is the default value.
     * @param activatedJob  job passed to the worker
     * @return the value as String
     */
    public Object getValue(String parameterName, Object defaultValue, ActivatedJob activatedJob) {
        if (!containsKeyInJob(parameterName, activatedJob))
            return getDefaultValue(parameterName, defaultValue);
        try {
            return getValueFromJob(parameterName, activatedJob);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Return the defaultValue for a parameter. If the defaultValue is provided by the software, it has the priority.
     * Else the default value is the one given in the parameter.
     *
     * @param parameterName name of parameter
     * @param defaultValue  default value given by the software
     * @return the default value
     */
    private Object getDefaultValue(String parameterName, Object defaultValue) {
        // if the software give a default value, it has the priority
        if (defaultValue != null)
            return defaultValue;
        List<RunnerParameter> inputFilter = listInput.stream().filter(t -> t.name.equals(parameterName)).toList();
        if (!inputFilter.isEmpty())
            return inputFilter.get(0).defaultValue;
        // definitively, no default value
        return null;
    }

    /**
     * Set the value. Worker must use this method, then the class can verify the output contract is respected
     *
     * @param parameterName name of the variable
     * @param value         value of the variable
     */
    public void setValue(String parameterName, Object value, AbstractWorker.ContextExecution contextExecution) {
        contextExecution.outVariablesValue.put(parameterName, value);
    }

    /**
     * Set a fileVariable value
     *
     * @param parameterName     name to save the fileValue
     * @param storageDefinition parameter which pilot the way to retrieve the value
     * @param fileVariableValue fileVariable to save
     */
    public void setFileVariableValue(String parameterName, String storageDefinition, FileVariable fileVariableValue, AbstractWorker.ContextExecution contextExecution) {
        try {
            FileVariableReference fileVariableReference = FileVariableFactory.getInstance().setFileVariable(storageDefinition, fileVariableValue);
            contextExecution.outVariablesValue.put(parameterName, fileVariableReference.toJson());
        } catch (Exception e) {
            logError("parameterName[" + parameterName + "] Error during setFileVariable read: " + e);
            throw new ZeebeBpmnError(BPMNERROR_SAVE_FILEVARIABLE, "Worker [" + getName() + "] error during access storageDefinition[" + storageDefinition + "] :" + e);
        }
    }

    /**
     * Log an error
     *
     * @param message message to log
     */
    public void logError(String message) {
        loggerAbstract.error("CherryWorker[" + getName() + "]: " + message);
    }

    /**
     * Check the contract
     * Each connector must and return a contract for what it needs for the execution
     *
     * @throws RuntimeException if the input is incorrect, contract not respected
     */
    protected void checkInput(final ActivatedJob job) throws RuntimeException {
        List<String> listErrors = new ArrayList<>();
        for (RunnerParameter parameter : getListInput()) {
            // if a parameter is a star, then this is not really a name
            if ("*".equals(parameter.name))
                continue;

            // value is in Variables if the designer map Input and Output manually
            // or may be in the custom headers if the designer use a template
            Object value = getValueFromJob(parameter.name, job);

            // check type
            if (value != null && incorrectClassParameter(value, parameter.clazz)) {
                listErrors.add("Param[" + parameter.name + "] expect class[" + parameter.clazz.getName() + "] received[" + value.getClass() + "];");
            }


            // check REQUIRED parameters
            if ((value == null || value.toString().trim().length() == 0) && parameter.level == RunnerParameter.Level.REQUIRED) {
                listErrors.add("Param[" + parameter.name + "] is missing");
            }
        }
        if (!listErrors.isEmpty()) {
            logError("CherryConnector[" + getType() + "] Errors:" + String.join(",", listErrors));
            throw new ZeebeBpmnError("INPUT_CONTRACT_ERROR", "Worker [" + getType() + "] InputContract Exception:" + String.join(",", listErrors));
        }
    }

    /**
     * Check the contract at output
     * The connector must use setVariable to set any value. Then, we can verify that all expected information are provided
     *
     * @param contextExecution keep the context of this execution
     * @throws RuntimeException when the contract is not respected
     */
    protected void checkOutput(AbstractWorker.ContextExecution contextExecution) throws RuntimeException {
        List<String> listErrors = new ArrayList<>();

        for (RunnerParameter parameter : getListOutput()) {

            // no check on the * parameter
            if ("*".equals(parameter.name))
                continue;

            if (parameter.level == RunnerParameter.Level.REQUIRED && !contextExecution.outVariablesValue.containsKey(parameter.name)) {
                listErrors.add("Param[" + parameter.name + "] is missing");
            }
            // if the value is given, it must be the correct value
            if (contextExecution.outVariablesValue.containsKey(parameter.name)) {
                Object value = contextExecution.outVariablesValue.get(parameter.name);

                if (incorrectClassParameter(value == null ? null : value.getClass().getName(), parameter.clazz))
                    listErrors.add("Param[" + parameter.name + "] expect class[" + parameter.clazz.getName()
                            + "] received[" + contextExecution.outVariablesValue.get(parameter.name).getClass() + "];");

            }
        }
        Set<String> outputName = getListOutput().stream().map(t -> t.name).collect(Collectors.toSet());
        // second pass: verify that the connector does not provide an unexpected value
        // if a outputParameter is "*" then the connector allows itself to produce anything
        long containsStar = getListOutput().stream().filter(t -> "*".equals(t.name)).count();
        if (containsStar == 0) {
            List<String> listExtraVariables = contextExecution.outVariablesValue.keySet()
                    .stream()
                    .filter(variable -> !outputName.contains(variable))
                    .toList();
            if (!listExtraVariables.isEmpty())
                listErrors.add("Output not defined in the contract[" + String.join(",", listExtraVariables) + "]");
        }


        if (!listErrors.isEmpty()) {
            logError("Errors:" + String.join(",", listErrors));
            throw new ZeebeBpmnError("OUTPUT_CONTRACT_ERROR", "Worker[" + getType() + "] OutputContract Exception:" + String.join(",", listErrors));
        }
    }



    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Runner parameters                                       */
    /*                                                          */
    /* Runner must declare the input/output parameters          */
    /* -------------------------------------------------------- */
    // If inputs and/or outputs are mapped as literals in the bpmn process diagram, the types are ambiguous. For example,
    // the value of `90` will be interpreted as an Integer, but we also need a way to interpret as a Long.

    // This idea was inspired by: https://stackoverflow.com/questions/40402756/check-if-a-string-is-parsable-as-another-java-type
    static Map<Class<?>, Predicate<String>> canParsePredicates = new HashMap<>();
    static {
        canParsePredicates.put(java.lang.Integer.class, s -> {try {Integer.parseInt(s); return true;} catch(Exception e) {return false;}});
        canParsePredicates.put(java.lang.Long.class, s -> {try {Long.parseLong(s); return true;} catch(Exception e) {return false;}});
    }

    static Boolean canParse(Class<?> clazz, Object value) {
        if(value != null) {
            return canParsePredicates.get(clazz).test(value.toString());
        } else {
            return false;
        }
    }
    /**
     * Check the object versus the expected parameter
     *
     * @param value        object value to check
     * @param isInstanceOf expected class
     * @return false if the value is on the class, else true
     */
    boolean incorrectClassParameter(Object value, Class<?> isInstanceOf) {
        if (value == null)
            return false;
        try {
            if (Class.forName(isInstanceOf.getName()).isInstance(value)) {
                return false;
            } else {
                return !canParse(isInstanceOf, value.toString());
            }
        } catch (Exception e) {
            // do nothing, we return true
        }
        return true;
    }


    private boolean containsKeyInJob(String parameterName, final ActivatedJob activatedJob) {
        return (activatedJob.getVariablesAsMap().containsKey(parameterName)
                || activatedJob.getCustomHeaders().containsKey(parameterName));
    }

    /**
     * Value is in Variables if the designer map Input and Output manually,
     * or may be in the custom headers if the designer use a template
     *
     * @param parameterName parameter to get the value
     * @param activatedJob  activated job
     * @return
     */
    protected Object getValueFromJob(String parameterName, final ActivatedJob activatedJob) {
        if (activatedJob.getVariablesAsMap().containsKey(parameterName))
            return activatedJob.getVariablesAsMap().get(parameterName);
        return activatedJob.getCustomHeaders().get(parameterName);
    }

    /**
     * Retrieve a variable, and return the string representation. If the variable is not a String, then a toString() is returned. If the value does not exist, then defaultValue is returned
     * The method can return null if the variable exists, but it is a null value.
     *
     * @param parameterName name of the variable to load
     * @param defaultValue  if the input does not exist, this is the default value.
     * @param activatedJob  job passed to the worker
     * @return the value as String
     */
    public String getInputStringValue(String parameterName, String defaultValue, final ActivatedJob activatedJob) {
        if (!containsKeyInJob(parameterName, activatedJob))
            return (String) getDefaultValue(parameterName, defaultValue);
        Object value = getValueFromJob(parameterName, activatedJob);
        return value == null ? null : value.toString();
    }



    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Getter/Setter                                           */
    /*                                                          */
    /* -------------------------------------------------------- */

    /* isLog
     * return if the  worker will log
     */
    public boolean isLog() {
        return isLogWorker;
    }

    public void setLog(boolean logWorker) {
        isLogWorker = logWorker;
    }


    public String getType() {
        return type;
    }

    public List<RunnerParameter> getListInput() {
        return listInput;
    }

    public void setListInput(List<RunnerParameter> listInput) {
        this.listInput = listInput;
    }

    public List<RunnerParameter> getListOutput() {
        return listOutput;
    }

    public void setListOutput(List<RunnerParameter> listOutput) {
        this.listOutput = listOutput;
    }

    public List<BpmnError> getListBpmnErrors() {
        return listBpmnErrors;
    }

    /**
     * Return the list of variable to fetch if this is possible, else null.
     * To calculate the list:
     * - the listInput must not be null (it may be empty)
     * - any input must be a STAR. A star in the input means the worker/connector ask to retrieve any information
     *
     * @return null if this is not possible to fetch variable, else the list of variable to fetch
     */
    public List<String> getListFetchVariables() {
        if (listInput == null)
            return null;
        boolean isStarPresent = listInput.stream().anyMatch(t -> t.name.equals("*"));
        if (isStarPresent)
            return null;
        return listInput.stream().map(RunnerParameter::getName).toList();
    }

    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Additional optional information                        */
    /*                                                          */
    /* -------------------------------------------------------- */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the identification for the worker: it is the name or, if this not exist, the type
     *
     * @return the identification
     */
    public String getIdentification() {
        return name == null ? type : name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }


}
