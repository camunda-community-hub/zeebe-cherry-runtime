/* ******************************************************************** */
/*                                                                      */
/*  FileVariableJSON                                                    */
/*                                                                      */
/*  Save a file variable in JSON, in the Camunda Engine                 */
/*  File are encoded in JSON                                            */
/* ******************************************************************** */
package org.camunda.vercors.definition.filevariable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileVariableJSON {
    Logger logger = LoggerFactory.getLogger(FileVariableJSON.class.getName());

    /**
     * Save the file Variable structure as JSON
     *
     * @param fileVariable fileVariable to generate the JSON value
     */
    public String toJson(FileVariable fileVariable) throws JsonProcessingException {
        try {
            return new ObjectMapper().writeValueAsString(fileVariable);
        } catch (JsonProcessingException e) {
            logger.error("Vercors.FileVariableJSON: exception " + e + " During serialize fileVariable");
            throw e;
        }
    }

    /**
     * deserialize a fileVariable
     *
     * @param fileVariableJson FileVariable in JSON format
     * @return the fileVariable object
     * @throws JsonProcessingException error during parsing the JSON information
     */
    public FileVariable fromJson(String fileVariableJson) throws JsonProcessingException {
        try {
            return new ObjectMapper().readValue(fileVariableJson, FileVariable.class);
        } catch (JsonProcessingException e) {
            logger.error("Vercors.FileVariableJSON: exception " + e + " During unserialize fileVariable");
            throw e;
        }
    }
}
