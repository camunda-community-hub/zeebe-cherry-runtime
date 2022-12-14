/* ******************************************************************** */
/*                                                                      */
/*  FileVariableJSON                                                    */
/*                                                                      */
/*  Save a file variable in JSON, in the Camunda Engine                 */
/*  File are encoded in JSON                                            */
/* ******************************************************************** */
package io.camunda.file.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageJSON extends Storage {
    Logger logger = LoggerFactory.getLogger(StorageJSON.class.getName());


    public StorageJSON(StorageDefinition storageDefinition, FileRepoFactory fileRepoFactory) {
        super(storageDefinition, fileRepoFactory);

    }

    @Override
    public String getName() {
        return "JSON";
    }


    public static String getStorageDefinitionString( ) {
        return StorageDefinition.StorageDefinitionType.JSON.name();
    }


        /**
         * Save the file Variable structure as JSON
         *
         * @param fileVariable fileVariable to generate the JSON value
         * @param fileVariableReferenceUpdated fileVariableReference to update
         */
    public FileVariableReference toStorage(FileVariable fileVariable, FileVariableReference fileVariableReferenceUpdated)
        throws Exception {
        try {
            FileVariableReference fileVariableReference = new FileVariableReference();
            fileVariableReference.storageDefinition = getStorageDefinition().encodeToString();
            fileVariableReference.content = new ObjectMapper().writeValueAsString(fileVariable);
            return fileVariableReference;
        } catch (JsonProcessingException e) {
            logger.error(getFileRepoFactory().getLoggerHeaderMessage(StorageJSON.class)+"exception " + e + " During serialize fileVariable");
            throw e;
        }
    }

    /**
     * deserialize a fileVariable
     *
     * @param fileVariableReference FileVariable in JSON format
     * @return the fileVariable object
     * @throws JsonProcessingException error during parsing the JSON information
     */
    public FileVariable fromStorage(FileVariableReference fileVariableReference) throws Exception {
        try {
            FileVariable fileVariable= new ObjectMapper().readValue( (String) fileVariableReference.content, FileVariable.class);
            fileVariable.setStorageDefinition( getStorageDefinition());
            return fileVariable;
        } catch (JsonProcessingException e) {
            logger.error(getFileRepoFactory().getLoggerHeaderMessage(StorageJSON.class)+"Exception " + e + " During unserialize fileVariable");
            throw e;
        }
    }

    /**
     * Nothing to do here
     * @param fileVariableReference reference to purge
     * @return true if everything is correct
     * @throws Exception if any error arrive
     */
    @Override
    public boolean purgeStorage(FileVariableReference fileVariableReference) throws Exception {
        return true;
    }

}
