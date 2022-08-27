/* ******************************************************************** */
/*                                                                      */
/*  Abstract Worker                                                     */
/*                                                                      */
/*  All workers extends this class. It gives tool to access parameters, */
/*  and the contract implementation on parameters                       */
/* ******************************************************************** */
package org.camunda.cherry.definition;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.camunda.cherry.definition.filevariable.FileVariable;
import org.camunda.cherry.definition.filevariable.FileVariableFactory;
import org.camunda.cherry.definition.filevariable.FileVariableReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractWorker {

    public final static String BPMNERROR_ACCESS_FILEVARIABLE = "ACCESS_FILEVARIABLE";
    public final static String BPMNERROR_SAVE_FILEVARIABLE = "SAVE_FILEVARIABLE";
    private final String name;
    private final List<WorkerParameter> listInput;
    private final List<WorkerParameter> listOutput;
    private final List<BpmnError> listBpmnErrors;
    Logger loggerAbstract = LoggerFactory.getLogger(AbstractWorker.class.getName());
    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Administration                                          */
    /*                                                          */
    /* -------------------------------------------------------- */
    private boolean isLogWorker = false;

    /**
     * Constructor
     *
     * @param name           name of the worker
     * @param listInput      list of Input parameters for the worker
     * @param listOutput     list of Output parameters for the worker
     * @param listBpmnErrors list of potential BPMN Error the worker can generate
     */

    protected AbstractWorker(String name,
                             List<WorkerParameter> listInput,
                             List<WorkerParameter> listOutput,
                             List<BpmnError> listBpmnErrors) {

        this.name = name;
        this.listInput = listInput;
        this.listOutput = listOutput;
        this.listBpmnErrors = listBpmnErrors;
    }

    /**
     * return the name of the worker
     *
     * @return the name of the worker
     */
    public String getName() {
        return name;
    }

    /**
     * return the list of Input parameters for this worker
     *
     * @return list of parameters
     */
    public List<WorkerParameter> getListInput() {
        return listInput;
    }

    /**
     * return the list of Output parameters for this worker
     *
     * @return list of parameters
     */
    public List<WorkerParameter> getListOutput() {
        return listOutput;
    }

    /**
     * return the list of BPMN error
     *
     * @return list of errors
     */
    public List<BpmnError> getListBpmnErrors() {
        return listBpmnErrors;
    }

    /**
     * The connector must call immediately this method, which is the skeleton for all executions
     * Attention: this is a Spring Component, so this is the same object called.
     *
     * @param jobClient    connection to Zeebe
     * @param activatedJob information on job to execute
     */
    public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {

        ContextExecution contextExecution = new ContextExecution();
        contextExecution.beginExecution = System.currentTimeMillis();

        // log input
        String logInput = listInput.stream()
                .map(t -> {

                    Object value = getValueFromJob(t.name, activatedJob);
                    if (value != null && value.toString().length() > 15)
                        value = value.toString().substring(0, 15) + "...";
                    return t.name + "=[" + value + "]";
                })
                .collect(Collectors.joining(","));
        if (isLog())
            logInfo("Start " + logInput);
        // first, see if the process respect the contract for this connector
        checkInput(activatedJob);

        // ok, this is correct, execute it now
        execute(jobClient, activatedJob, contextExecution);

        // let's verify the execution respect the output contract
        checkOutput(contextExecution);


        // save the output in the process instance
        jobClient.newCompleteCommand(activatedJob.getKey()).variables(contextExecution.outVariablesValue).send().join();

        contextExecution.endExecution = System.currentTimeMillis();
        if (isLog())
            logInfo("End in " + (contextExecution.endExecution - contextExecution.beginExecution) + " ms");
        else if (contextExecution.endExecution - contextExecution.beginExecution > 2000)
            logInfo("End in " + (contextExecution.endExecution - contextExecution.beginExecution) + " ms (long)");
    }


    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Log worker                                             */
    /*                                                          */
    /* to normalize the log use these methods
    /* -------------------------------------------------------- */

    /**
     * Worker must implement this method. Real job has to be done here.
     *
     * @param jobClient        connection to Zeebe
     * @param activatedJob     information on job to execute
     * @param contextExecution the same object is used for all call. The contextExecution is an object for each execution
     */
    public abstract void execute(final JobClient jobClient, final ActivatedJob activatedJob, ContextExecution contextExecution);

    /**
     * log info
     *
     * @param message message to log
     */
    public void logInfo(String message) {
        loggerAbstract.info("CherryWorker[" + getName() + "]:" + message);
    }


    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Contracts operation on input/output                     */
    /*                                                          */
    /* -------------------------------------------------------- */

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
    private void checkInput(final ActivatedJob job) throws RuntimeException {
        List<String> listErrors = new ArrayList<>();
        for (WorkerParameter parameter : listInput) {
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
            if ((value == null || value.toString().trim().length() == 0) && parameter.level == Level.REQUIRED) {
                listErrors.add("Param[" + parameter.name + "] is missing");
            }
        }
        if (!listErrors.isEmpty()) {
            logError("CherryConnector[" + name + "] Errors:" + String.join(",", listErrors));
            throw new ZeebeBpmnError("INPUT_CONTRACT_ERROR", "Worker [" + name + "] InputContract Exception:" + String.join(",", listErrors));
        }
    }

    /**
     * Check the contract at output
     * The connector must use setVariable to set any value. Then, we can verify that all expected information are provided
     *
     * @param contextExecution keep the context of this execution
     * @throws RuntimeException when the contract is not respected
     */
    private void checkOutput(ContextExecution contextExecution) throws RuntimeException {
        List<String> listErrors = new ArrayList<>();

        for (WorkerParameter parameter : listOutput) {

            // no check on the * parameter
            if ("*".equals(parameter.name))
                continue;

            if (parameter.level == Level.REQUIRED && !contextExecution.outVariablesValue.containsKey(parameter.name)) {
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
        Set<String> outputName = listOutput.stream().map(t -> t.name).collect(Collectors.toSet());
        // second pass: verify that the connector does not provide an unexpected value
        // if a outputParameter is "*" then the connector allows itself to produce anything
        long containsStar = listOutput.stream().filter(t -> "*".equals(t.name)).count();
        if (containsStar == 0) {
            List<String> listExtraVariables = contextExecution.outVariablesValue.keySet()
                    .stream()
                    .filter(variable -> !outputName.contains(variable))
                    .collect(Collectors.toList());
            if (!listExtraVariables.isEmpty())
                listErrors.add("Output not defined in the contract[" + String.join(",", listExtraVariables) + "]");
        }


        if (!listErrors.isEmpty()) {
            logError("Errors:" + String.join(",", listErrors));
            throw new ZeebeBpmnError("OUTPUT_CONTRACT_ERROR", "Worker[" + name + "] OutputContract Exception:" + String.join(",", listErrors));
        }
    }

    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Worker parameters                                       */
    /*                                                          */
    /* Worker must declare the input/output parameters          */
    /* -------------------------------------------------------- */

    /**
     * Check the object versus the expected parameter
     *
     * @param value        object value to check
     * @param isInstanceOf expected class
     * @return false if the value is on the class, else true
     */
    private boolean incorrectClassParameter(Object value, Class<?> isInstanceOf) {
        if (value == null)
            return false;
        try {
            if (Class.forName(isInstanceOf.getName()).isInstance(value))
                return false;
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
    private Object getValueFromJob(String parameterName, final ActivatedJob activatedJob) {
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
        if (value instanceof Double)
            return (Double) value;
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
        if (value instanceof Map)
            return (Map) value;

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
        if (value instanceof Long)
            return (Long) value;
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
        if (value instanceof Duration)
            return (Duration) value;
        if (value instanceof Long)
            return Duration.ofMillis((Long) value);
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
        List<WorkerParameter> inputFilter = listInput.stream().filter(t -> t.name.equals(parameterName)).collect(Collectors.toList());
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
    public void setValue(String parameterName, Object value, ContextExecution contextExecution) {
        contextExecution.outVariablesValue.put(parameterName, value);
    }

    /**
     * Set a fileVariable value
     *
     * @param parameterName     name to save the fileValue
     * @param storageDefinition parameter which pilot the way to retrieve the value
     * @param fileVariableValue fileVariable to save
     */
    public void setFileVariableValue(String parameterName, String storageDefinition, FileVariable fileVariableValue, ContextExecution contextExecution) {
        try {
            FileVariableReference fileVariableReference = FileVariableFactory.getInstance().setFileVariable(storageDefinition, fileVariableValue);
            contextExecution.outVariablesValue.put(parameterName, fileVariableReference.toJson());
        } catch (Exception e) {
            logError("parameterName[" + parameterName + "] Error during setFileVariable read: " + e);
            throw new ZeebeBpmnError(BPMNERROR_SAVE_FILEVARIABLE, "Worker [" + getName() + "] error during access storageDefinition[" + storageDefinition + "] :" + e);
        }
    }

    /* isLog
     * return if the  worker will log
     */
    public boolean isLog() {
        return isLogWorker;
    }

    public void setLog(boolean logWorker) {
        isLogWorker = logWorker;
    }


    /**
     * Level on the parameter.
     */
    public enum Level {REQUIRED, OPTIONAL}

    /**
     * Parameter may define a list of choice.
     */
    public static class WorkerParameterChoice {
        public String name;
        public String label;

        public WorkerParameterChoice(String name, String label) {
            this.name = name;
            this.label = label;
        }
    }

    /**
     * class to declare a parameter
     */
    public static class WorkerParameter {
        public String name;
        public String label;
        public Class<?> clazz;
        public Object defaultValue;
        public Level level;
        public String explanation;


        /**
         * Declare a condition on the parameters
         */
        public String conditionPropertie;
        public List<String> conditionOneOf;


        public List<WorkerParameterChoice> workerParameterChoiceList;

        /**
         * Get an instance without a default value
         *
         * @param parameterName parameter name
         * @param clazz         class of the expected parameter
         * @param defaultValue  the default value for this parameter, if no value is given. Note: a required parameter may have a null as a value.
         * @param level         level for this parameter
         * @param explanation   describe the usage of the parameter
         * @return a WorkerParameter
         */
        public static WorkerParameter getInstance(String parameterName, String parameterLabel, Class<?> clazz, Object defaultValue, Level level, String explanation) {
            WorkerParameter parameter = new WorkerParameter();
            parameter.name = parameterName;
            parameter.label = parameterLabel;
            parameter.clazz = clazz;
            parameter.defaultValue = defaultValue;
            parameter.level = level;
            parameter.explanation = explanation;
            return parameter;
        }

        /**
         * Get an instance without a default value
         *
         * @param parameterName  parameter name
         * @param parameterLabel label to display in the template
         * @param clazz          class of the expected parameter
         * @param level          level for this parameter
         * @param explanation    describe the usage of the parameter
         * @return a WorkerParameter
         */
        public static WorkerParameter getInstance(String parameterName, String parameterLabel, Class<?> clazz, Level level, String explanation) {
            WorkerParameter parameter = new WorkerParameter();
            parameter.name = parameterName;
            parameter.label = parameterLabel;
            parameter.clazz = clazz;
            parameter.defaultValue = null;
            parameter.level = level;
            parameter.explanation = explanation;
            return parameter;
        }

        public WorkerParameter addCondition(String propertie, List<String> oneOf) {
            this.conditionPropertie = propertie;
            this.conditionOneOf = oneOf;
            return this;
        }

        /**
         * Worker can define a list of choice. Add a new choice in the list
         *
         * @param name
         * @param label
         * @return
         */
        public WorkerParameter addChoice(String name, String label) {
            if (workerParameterChoiceList == null)
                workerParameterChoiceList = new ArrayList<>();
            workerParameterChoiceList.add(new WorkerParameterChoice(name, label));
            return this;
        }


    }

    /**
     * Describe a BPMNError : code and explanation
     */
    public static class BpmnError {
        public String code;
        public String explanation;

        /**
         * Create a Bpmn Error explanation
         *
         * @param code
         * @param explanation
         * @return
         */
        public static BpmnError getInstance(String code, String explanation) {
            BpmnError bpmnError = new BpmnError();
            bpmnError.code = code;
            bpmnError.explanation = explanation;
            return bpmnError;
        }

    }

    /**
     * All executions call the same object. This contains all the context for one execution.
     */
    protected class ContextExecution {
        public final Map<String, Object> outVariablesValue = new HashMap<>();
        long beginExecution;
        long endExecution;


    }

}
