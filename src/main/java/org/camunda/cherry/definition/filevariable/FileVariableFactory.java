/* ******************************************************************** */
/*                                                                      */
/*  FileVariableFactory                                                 */
/*                                                                      */
/*  File can't be save in C8. So, different implementation to store     */
/*  files are possible, and the factory give access to the different    */
/* formats                                                              */
/* ******************************************************************** */
package org.camunda.cherry.definition.filevariable;

import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringTokenizer;

public class FileVariableFactory {
    Logger logger = LoggerFactory.getLogger(FileVariableFactory.class.getName());

    public final static String BPMNERROR_INCORRECT_STORAGEDEFINITION = "INCORRECT_STORAGEDEFINITION";

    /**
     * Use the getInstance() method
     */
    private FileVariableFactory() {
    }

    public static FileVariableFactory getInstance() {
        return new FileVariableFactory();
    }

    /**
     * get the FileVariable object from the different information
     * StorageDefinition is a string like
     * "JSON" : the value is a JSON information, to be unSerialize
     * "TEMPORARYDISK:<path>", and the value is a file name in this directory.
     *
     * @param fileContainer information to access the file
     * @return a fileVariable
     * @throws Exception can't load the fileVariable
     */
    public FileVariable getFileVariable(FileVariableReference fileContainer) throws Exception {
        if (fileContainer == null || fileContainer.content == null)
            return null;

        FileVariableStorage storageType = extractStorageType(fileContainer.storageDefinition);

        switch (storageType) {
            case FOLDER:
                return new FileVariableFolder().fromFolder(fileContainer.storageDefinition, fileContainer.content.toString());

            case TEMPFOLDER:
                return new FileVariableTempFolder().fromTempFolder(fileContainer.content.toString());

            case JSON:
            default:
                return new FileVariableJSON().fromJson(fileContainer.content.toString());
        }

    }

    /**
     * SetFileVariable
     *
     * @param storageDefinition storage Definition to pilot the way to save the value
     * @param fileVariableValue file Variable to save
     * @return the FileContainer (depends on the storageDefinition code)
     * @throws Exception if an error arrive
     */
    public FileVariableReference setFileVariable(String storageDefinition, FileVariable fileVariableValue) throws Exception {
        if (fileVariableValue == null)
            return null;

        FileVariableStorage storageType = extractStorageType(storageDefinition);
        if (storageType == null)
            throw new ZeebeBpmnError(BPMNERROR_INCORRECT_STORAGEDEFINITION, "Error during storageDefinition[" + storageDefinition + "]");

        FileVariableReference fileContainer = new FileVariableReference();
        fileContainer.storageDefinition = storageDefinition;

        switch (storageType) {
            case FOLDER:
                fileContainer.content = new FileVariableFolder().toFolder(storageDefinition, fileVariableValue);
                break;
            case TEMPFOLDER:
                fileContainer.content = new FileVariableTempFolder().toTempFolder(fileVariableValue);
                break;
            case JSON:
            default:
                fileContainer.content = new FileVariableJSON().toJson(fileVariableValue);
        }
        return fileContainer;
    }

    /**
     * Extract the storageType from the storageDefinition connection string
     *
     * @param storageDefinition the storage definition string
     * @return the storage type
     */
    private FileVariableStorage extractStorageType(String storageDefinition) {
        try {
            StringTokenizer st = new StringTokenizer(storageDefinition, ":");

            String storageTypeSt = st.hasMoreTokens() ? st.nextToken() : null;
            return FileVariableStorage.valueOf(storageTypeSt);
        } catch (Exception e) {
            String message="Can't decode storageDefinition [" + storageDefinition + "]. Format should be ["
                    + FileVariableStorage.JSON.toString()
                    + "|" + FileVariableStorage.TEMPFOLDER.toString()
                    +  "|" +FileVariableStorage.FOLDER.toString()+"]";
            logger.error("Cherry.FileVariableFactory: "+message);
            throw new ZeebeBpmnError(BPMNERROR_INCORRECT_STORAGEDEFINITION, message);
        }
    }

    /**
     * Define how the file variable is stored.
     * JSON: easy, but attention to large file
     */
    public enum FileVariableStorage {JSON, TEMPFOLDER, FOLDER}
}
