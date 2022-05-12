package org.camunda.vercors.definition;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractWorker {

    Logger logger = LoggerFactory.getLogger(AbstractWorker.class.getName());

    private final String name;
    private final List<WorkerParameter> listInput;
    private final List<WorkerParameter> listOutput;
    private final Map<String, String> outVariables = new HashMap<>();

    /**
     * Constructor
     *
     * @param name       name of the worker
     * @param listInput  list of Input parameters for the worker
     * @param listOutput list of Output parameters for the worker
     */
    public AbstractWorker(String name,
                          List<WorkerParameter> listInput,
                          List<WorkerParameter> listOutput) {
        this.name = name;
        this.listInput = listInput;
        this.listOutput = listOutput;
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
     * The connector must call immediately this method, which is the skeleton for all executions
     *
     * @param jobClient    connection to Zeebe
     * @param activatedJob information on job to execute
     */
    public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {
        long beginExecution = System.currentTimeMillis();
        logger.info("Vercors-" + name + ": Start");
        // first, see if the process respect the contract for this connector
        checkInput(activatedJob);

        // ok, this is correct, execute it now
        execute(jobClient, activatedJob);

        // let's verify the execution respect the output contract
        checkOutput();
        long endExecution = System.currentTimeMillis();
        logger.info("Vercors-" + name + ": End in " + (endExecution - beginExecution));
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
            logger.error("VercorsConnector[" + name + "] Errors:" + String.join(",", listErrors));
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
                if (!outVariables.containsKey(parameter.name))
                    listErrors.add("Param[" + parameter.name + "] is missing");
            }
            // if the value is given, it must be the correct value
            if (outVariables.containsKey(parameter.name)) {
                if (incorrectClassParameter(outVariables.get(parameter.name), parameter.clazz))
                    listErrors.add("Param[" + parameter.name + "] expect class[" + parameter.clazz.getName()
                            + "] received[" + outVariables.get(parameter.name).getClass() + "];");
            }
        }
        Set<String> outputName = listOutput.stream().map(t -> t.name).collect(Collectors.toSet());
        // second pass: verify that the connector does not provide an unexpected value
        List<String> listExtraVariables = outVariables.keySet()
                .stream()
                .filter(outputName::contains)
                .collect(Collectors.toList());
        if (!listExtraVariables.isEmpty())
            listErrors.add("Output not defined in the contract[" + String.join(",", listExtraVariables) + "]");

        if (!listErrors.isEmpty()) {
            logger.error("VercorsConnector[" + name + "] Errors:" + String.join(",", listErrors));
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
     * Level on the parameter.
     */
    public enum Level {REQUIRED, OPTIONAL}

    /**
     * class to declare a parameter
     */
    public static class WorkerParameter {
        public String name;
        public Class<?> clazz;
        public Level level;

        public static WorkerParameter getInstance(String name, Class<?> clazz, Level level) {
            WorkerParameter parameter = new WorkerParameter();
            parameter.name = name;
            parameter.clazz = clazz;
            parameter.level = level;
            return parameter;
        }
    }

    /* -------------------------------------------------------- */
    /*                                                          */
    /*  getInput/setOutput                                      */
    /*                                                          */
    /* method to get variable value                             */
    /* -------------------------------------------------------- */

    /**
     * Retrieve a variable, and return the string representation. If the variable is not a String, then a toString() is returned. If the value does not exist, then defaultValue is returned
     * The method can return null if the variable exists, but it is a null value.
     *
     * @param name         name of the variable to load
     * @param defaultValue if the input does not exist, this is the default value.
     * @param activatedJob job passed to the worker
     * @return the value as String
     */
    public String getInputStringValue(String name, String defaultValue, final ActivatedJob activatedJob) {
        if (!activatedJob.getVariablesAsMap().containsKey(name))
            return defaultValue;
        Object value = activatedJob.getVariablesAsMap().get(name);
        return value == null ? null : value.toString();
    }

    /**
     * Return a value as Double
     *
     * @param name         name of the variable
     * @param defaultValue default value, if the variable does not exist or any error arrived (can't parse the value)
     * @param activatedJob job passed to the worker
     * @return a Double value
     */
    public Double getInputDoubleValue(String name, Double defaultValue, final ActivatedJob activatedJob) {
        if (!activatedJob.getVariablesAsMap().containsKey(name))
            return defaultValue;
        Object value = activatedJob.getVariablesAsMap().get(name);
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
     * Return a value as Long
     *
     * @param name         name of the variable
     * @param defaultValue default value, if the variable does not exist or any error arrived (can't parse the value)
     * @param activatedJob job passed to the worker
     * @return a Double value
     */
    public Long getInputLongValue(String name, Long defaultValue, final ActivatedJob activatedJob) {
        if (!activatedJob.getVariablesAsMap().containsKey(name))
            return defaultValue;
        Object value = activatedJob.getVariablesAsMap().get(name);
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
     * @param name         name of the variable
     * @param defaultValue default value, if the variable does not exist or any error arrived (can't parse the value)
     * @param activatedJob job passed to the worker
     * @return a Double value
     */
    public Duration getInputDurationValue(String name, Duration defaultValue, final ActivatedJob activatedJob) {
        if (!activatedJob.getVariablesAsMap().containsKey(name))
            return defaultValue;
        Object value = activatedJob.getVariablesAsMap().get(name);
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
     * return a variable value
     *
     * @param name         name of the input value
     * @param defaultValue if the input does not exist, this is the default value.
     * @param activatedJob job passed to the worker
     * @return the value as String
     */
    public Object getValue(String name, Object defaultValue, ActivatedJob activatedJob) {
        if (!activatedJob.getVariablesAsMap().containsKey(name))
            return defaultValue;
        try {
            return activatedJob.getVariablesAsMap().get(name);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Set the value. Worker must use this method, then the class can verify the output contract is respected
     *
     * @param name  name of the variable
     * @param value value of the variable
     */
    public void setValue(String name, Object value) {
        outVariables.put(name, value == null ? null : value.getClass().getName());
    }
}
