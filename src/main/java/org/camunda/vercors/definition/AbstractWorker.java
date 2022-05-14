/* ******************************************************************** */
/*                                                                      */
/*  Abstract Worker                                                     */
/*                                                                      */
/*  All workers extends this class. It gives tool to access parameters, */
/*  and the contract implementation on parameters                       */
/* ******************************************************************** */
package org.camunda.vercors.definition;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.camunda.vercors.definition.filevariable.FileVariable;
import org.camunda.vercors.definition.filevariable.FileVariableFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractWorker {

    private final String name;
    private final List<WorkerParameter> listInput;
    private final List<WorkerParameter> listOutput;
    private final List<String> listBpmnErrors;
    private final Map<String, Object> outVariablesValue = new HashMap<>();
    Logger logger = LoggerFactory.getLogger(AbstractWorker.class.getName());


    /**
     * Constructor
     *
     * @param name           name of the worker
     * @param listInput      list of Input parameters for the worker
     * @param listOutput     list of Output parameters for the worker
     * @param listBpmnErrors list of potential BPMN Error the worker can generate
     */
    public AbstractWorker(String name,
                          List<WorkerParameter> listInput,
                          List<WorkerParameter> listOutput,
                          List<String> listBpmnErrors) {
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


    public List<WorkerParameter> getListInput() {
        return listInput;
    }

    public List<WorkerParameter> getListOutput() {
        return listOutput;
    }

    public List<String> getListBpmnErrors() {
        return listBpmnErrors;
    }

    /**
     * The connector must call immediately this method, which is the skeleton for all executions
     *
     * @param jobClient    connection to Zeebe
     * @param activatedJob information on job to execute
     */
    public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {
        long beginExecution = System.currentTimeMillis();
        // log input
        String logInput = listInput.stream()
                .map(t -> {
                    Object value = activatedJob.getVariablesAsMap().get(t.name);
                    if (value != null && value.toString().length() > 15)
                        value = value.toString().substring(0, 15) + "...";
                    return t.name + "=[" + value + "]";
                })
                .collect(Collectors.joining(","));
        logInfo("Start " + logInput);
        // first, see if the process respect the contract for this connector
        checkInput(activatedJob);

        // ok, this is correct, execute it now
        execute(jobClient, activatedJob);

        // let's verify the execution respect the output contract
        checkOutput();


        // save the output in the process instance
        jobClient.newCompleteCommand(activatedJob.getKey()).variables(outVariablesValue).send().join();

        long endExecution = System.currentTimeMillis();
        logInfo("End in " + (endExecution - beginExecution) + " ms");
    }

    /**
     * Worker must implement this method. Real job has to be done here.
     *
     * @param jobClient    connection to Zeebe
     * @param activatedJob information on job to execute
     */
    public abstract void execute(final JobClient jobClient, final ActivatedJob activatedJob);

    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Log worker                                             */
    /*                                                          */
    /* to normalize the log use these methods
    /* -------------------------------------------------------- */

    /**
     * log info
     *
     * @param message message to log
     */
    public void logInfo(String message) {
        logger.info("VercorsWorker[" + getName() + "]: " + message);
    }

    /**
     * Log an error
     *
     * @param message message to log
     */
    public void logError(String message) {
        logger.error("VercorsWorker[" + getName() + "]: " + message);
    }


    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Contracts operation on input/output                     */
    /*                                                          */
    /* -------------------------------------------------------- */

    /**
     * Check the contract
     * Each connector must and return a contract for what it needs for the execution
     *
     * @throws RuntimeException if the input is incorrect, contract not respected
     */
    private void checkInput(final ActivatedJob job) throws RuntimeException {
        List<String> listErrors = new ArrayList<>();
        for (WorkerParameter parameter : listInput) {
            if (job.getVariablesAsMap().containsKey(parameter.name)) {
                Object value = job.getVariablesAsMap().get(parameter.name);

                if (incorrectClassParameter(value, parameter.clazz)) {
                    listErrors.add("Param[" + parameter.name + "] expect class[" + parameter.clazz.getName() + "] received[" + value.getClass() + "];");
                }
            } else if (parameter.level == Level.REQUIRED) {
                listErrors.add("Param[" + parameter.name + "] is missing");
            }
        }
        if (!listErrors.isEmpty()) {
            logError("VercorsConnector[" + name + "] Errors:" + String.join(",", listErrors));
            throw new ZeebeBpmnError("INPUT_CONTRACT_ERROR", "Worker [" + name + "] InputContract Exception:" + String.join(",", listErrors));
        }
    }

    /**
     * Check the contract at output
     * The connector must use setVariable to set any value. Then, we can verify that all expected information are provided
     *
     * @throws RuntimeException when the contract is not respected
     */
    private void checkOutput() throws RuntimeException {
        List<String> listErrors = new ArrayList<>();

        for (WorkerParameter parameter : listOutput) {

            if (parameter.level == Level.REQUIRED) {
                if (!outVariablesValue.containsKey(parameter.name))
                    listErrors.add("Param[" + parameter.name + "] is missing");
            }
            // if the value is given, it must be the correct value
            if (outVariablesValue.containsKey(parameter.name)) {
                Object value = outVariablesValue.get(parameter.name);

                if (incorrectClassParameter(value == null ? null : value.getClass().getName(), parameter.clazz))
                    listErrors.add("Param[" + parameter.name + "] expect class[" + parameter.clazz.getName()
                            + "] received[" + outVariablesValue.get(parameter.name).getClass() + "];");
            }
        }
        Set<String> outputName = listOutput.stream().map(t -> t.name).collect(Collectors.toSet());
        // second pass: verify that the connector does not provide an unexpected value
        List<String> listExtraVariables = outVariablesValue.keySet()
                .stream()
                .filter(variable -> !outputName.contains(variable))
                .collect(Collectors.toList());
        if (!listExtraVariables.isEmpty())
            listErrors.add("Output not defined in the contract[" + String.join(",", listExtraVariables) + "]");

        if (!listErrors.isEmpty()) {
            logError("Errors:" + String.join(",", listErrors));
            throw new ZeebeBpmnError("OUTPUT_CONTRACT_ERROR", "Worker[" + name + "] OutputContract Exception:" + String.join(",", listErrors));
        }
    }

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
    /* -------------------------------------------------------- */
    /*                                                          */
    /*  Worker parameters                                       */
    /*                                                          */
    /* Worker must declare the input/output parameters          */
    /* -------------------------------------------------------- */

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
        if (!activatedJob.getVariablesAsMap().containsKey(parameterName))
            return (String) getDefaultValue(parameterName, defaultValue);
        Object value = activatedJob.getVariablesAsMap().get(parameterName);
        return value == null ? null : value.toString();
    }

    /**
     * Return a value as Double
     *
     * @param parameterName name of the variable
     * @param defaultValue  default value, if the variable does not exist or any error arrived (can't parse the value)
     * @param activatedJob  job passed to the worker
     * @return a Double value
     */
    public Double getInputDoubleValue(String parameterName, Double defaultValue, final ActivatedJob activatedJob) {
        if (!activatedJob.getVariablesAsMap().containsKey(parameterName))
            return (Double) getDefaultValue(parameterName, defaultValue);
        Object value = activatedJob.getVariablesAsMap().get(parameterName);
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

    /* -------------------------------------------------------- */
    /*                                                          */
    /*  getInput/setOutput                                      */
    /*                                                          */
    /* method to get variable value                             */
    /* -------------------------------------------------------- */

    /**
     * Return a value as Long
     *
     * @param parameterName name of the variable
     * @param defaultValue  default value, if the variable does not exist or any error arrived (can't parse the value)
     * @param activatedJob  job passed to the worker
     * @return a Double value
     */
    public Long getInputLongValue(String parameterName, Long defaultValue, final ActivatedJob activatedJob) {
        if (!activatedJob.getVariablesAsMap().containsKey(parameterName))
            return (Long) getDefaultValue(parameterName, defaultValue);
        Object value = activatedJob.getVariablesAsMap().get(parameterName);

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
        if (!activatedJob.getVariablesAsMap().containsKey(parameterName))
            return (Duration) getDefaultValue(parameterName, defaultValue);
        Object value = activatedJob.getVariablesAsMap().get(parameterName);
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
     * @param parameterName     name where the value is stored
     * @param storageDefinition parameter which pilot the way to retrieve the value
     * @param activatedJob      job passed to the worker
     * @return a FileVariable
     */
    public FileVariable getFileVariableValue(String parameterName, String storageDefinition, final ActivatedJob activatedJob) {
        if (!activatedJob.getVariablesAsMap().containsKey(parameterName))
            return null;
        Object value = activatedJob.getVariablesAsMap().get(parameterName);
        if (value == null)
            return null;
        try {
            return FileVariableFactory.getInstance().getFileVariable(storageDefinition, value);
        } catch (Exception e) {
            logger.error("VercorsConnector[" + name + "] Error during FileVariable read: " + e);
        }
        return null;
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
        if (!activatedJob.getVariablesAsMap().containsKey(parameterName))
            return getDefaultValue(parameterName, defaultValue);
        try {
            return activatedJob.getVariablesAsMap().get(parameterName);
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
    public void setValue(String parameterName, Object value) {
        outVariablesValue.put(parameterName, value);
    }

    /**
     * Set a fileVariable value
     *
     * @param parameterName     name to save the fileValue
     * @param storageDefinition parameter which pilot the way to retrieve the value
     * @param fileVariableValue fileVariable to save
     */
    public void setFileVariableValue(String parameterName, String storageDefinition, FileVariable fileVariableValue) {
        try {
            Object fileVariableEncoded = FileVariableFactory.getInstance().setFileVariable(storageDefinition, fileVariableValue);
            outVariablesValue.put(parameterName, fileVariableEncoded);
        } catch (Exception e) {
            logError("parameterName[" + parameterName + "] Error during setFileVariable read: " + e);
        }
    }

    /**
     * Level on the parameter.
     */
    public enum Level {REQUIRED, OPTIONAL}

    /**
     * class to declare a parameter
     */
    public static class WorkerParameter {
        public String name;
        public Class<?> clazz;
        public Object defaultValue;
        public Level level;
        public String explanation;

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
        public static WorkerParameter getInstance(String parameterName, Class<?> clazz, Object defaultValue, Level level, String explanation) {
            WorkerParameter parameter = new WorkerParameter();
            parameter.name = parameterName;
            parameter.clazz = clazz;
            parameter.defaultValue = defaultValue;
            parameter.level = level;
            return parameter;
        }

        /**
         * Get an instance without a default value
         *
         * @param parameterName parameter name
         * @param clazz         class of the expected parameter
         * @param level         level for this parameter
         * @param explanation   describe the usage of the parameter
         * @return a WorkerParameter
         */
        public static WorkerParameter getInstance(String parameterName, Class<?> clazz, Level level, String explanation) {
            WorkerParameter parameter = new WorkerParameter();
            parameter.name = parameterName;
            parameter.clazz = clazz;
            parameter.defaultValue = null;
            parameter.level = level;
            parameter.explanation = explanation;
            return parameter;
        }
    }
}