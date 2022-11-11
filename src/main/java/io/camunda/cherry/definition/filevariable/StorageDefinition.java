/* ******************************************************************** */
/*                                                                      */
/*  StorageDefinition                                                   */
/*                                                                      */
/*  The storage definition contains only the description to access the  */
/* storage, and not what it is saved inside.                            */
/* the Storage definition may bne simple (TEMPFOLDER) or complex        */
/*  for CMIS, the complete informatin to connect to the CMIS is part of */
/*  This class just manipulate the information with the format          */
/*  <Type>:<Complement>
/* ******************************************************************** */
package io.camunda.cherry.definition.filevariable;

import com.google.gson.Gson;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageDefinition {

    public final static String BPMNERROR_INCORRECT_STORAGEDEFINITION = "INCORRECT_STORAGEDEFINITION";
    static Logger logger = LoggerFactory.getLogger(StorageDefinition.class.getName());
    public StorageDefinitionType type;
    public String complement = null;
    public Object complementInObject = null;

    public static StorageDefinition getFromString(String completeStorageDefinition) throws ZeebeBpmnError {
        try {
            int posDelimiter = completeStorageDefinition.indexOf(":");

            String storageTypeSt = posDelimiter == -1 ? completeStorageDefinition : completeStorageDefinition.substring(0, posDelimiter);
            StorageDefinition storageDefinition = new StorageDefinition();
            storageDefinition.type = StorageDefinitionType.valueOf(storageTypeSt);

            switch( storageDefinition.type) {
                case FOLDER: storageDefinition.complement = completeStorageDefinition.substring(posDelimiter + 1);break;
                case CMIS:
                    String complement = completeStorageDefinition.substring(posDelimiter + 1);
                    Gson gson = new Gson();
                    storageDefinition.complementInObject= gson.fromJson(complement, Object.class);
                    break;
                default:
                    break;
            }

            return storageDefinition;
        } catch (Exception e) {
            String message = "Can't decode storageDefinition [" + completeStorageDefinition + "]. Format should be ["
                    + StorageDefinitionType.JSON
                    + "|" + StorageDefinitionType.CMIS
                    + "|" + StorageDefinitionType.TEMPFOLDER
                    + "|" + StorageDefinitionType.FOLDER + "]";
            logger.error("Cherry.StorageDefinition: Can't decode [" + completeStorageDefinition + "] " + e);
            throw new ZeebeBpmnError(BPMNERROR_INCORRECT_STORAGEDEFINITION, message);
        }
    }

    /**
     * Encode the current storage definition to a String, so it is easily movable to any information
     *
     * @return
     */
    public String encodeToString() {
        String result = type.toString();
        if (complement != null) {
            result += ":" + complement;
        } else if (complementInObject != null) {
            Gson gson = new Gson();
            result += ":" + gson.toJson(complementInObject);
        }
        return result;
    }

    /**
     * Define how the file variable is stored.
     * JSON: easy, but attention to large file
     */
    public enum StorageDefinitionType {JSON, TEMPFOLDER, FOLDER, CMIS}

}
