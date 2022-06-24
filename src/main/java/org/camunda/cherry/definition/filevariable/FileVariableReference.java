/* ******************************************************************** */
/*                                                                      */
/*  FileVariableReference                                               */
/*                                                                      */
/*  This object carry the reference to a file variable, not the file    */
/*  itself. This is used as a process variable.                         */
/*  The content may be retrieved via the FileVariableFactory            */
/*                                                                      */
/* ******************************************************************** */
package org.camunda.cherry.definition.filevariable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A FileContainer must be self-description. So, it contains the way to find the file itself (JSON, FOLDER...) and the information to access the content
 */
public class FileVariableReference {
    private static final Logger logger = LoggerFactory.getLogger(FileVariableReference.class.getName());

    /**
     * Storage definition where the file is
     */
    public String storageDefinition;
    /**
     * content to retrieve the file in the storageDefinition (key to access it)
     */
    public Object content;

    /**
     * Transform the reference from JSON
     *
     * @param fileReferenceJson file Reference in JSON
     * @return fileReference
     * @throws Exception
     */
    public static FileVariableReference fromJson(String fileReferenceJson) throws Exception {
        try {
            return new ObjectMapper().readValue(fileReferenceJson, FileVariableReference.class);
        } catch (JsonProcessingException e) {
            logger.error("Cherry.FileVariableReference.fromJson: exception " + e + " During un serialize fileVariable");
            throw e;
        }
    }

    /**
     * Transform the fileReference to JSON
     *
     * @return JSON
     * @throws JsonProcessingException
     */
    public String toJson() throws JsonProcessingException {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            logger.error("Cherry.FileVariableReference.toJson: exception " + e + " During serialize fileVariable");
            throw e;
        }
    }
}
