/* ******************************************************************** */
/*                                                                      */
/*  FileVariableFactory                                                 */
/*                                                                      */
/*  File can't be save in C8. So, different implementation to store     */
/*  files are possible, and the factory give access to the different    */
/* formats                                                              */
/* ******************************************************************** */
package org.camunda.vercors.definition.filevariable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringTokenizer;

public class FileVariableFactory {
    Logger logger = LoggerFactory.getLogger(FileVariableFactory.class.getName());

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
     * @param storageDefinition is the storage definition
     * @param value             value where the fileVariable information is stored
     * @return a fileVariable
     * @throws Exception can't load the fileVariable
     */
    public FileVariable getFileVariable(String storageDefinition, Object value) throws Exception {
        if (value == null)
            return null;

        FileVariableStorage storageType = extractStorageType(storageDefinition);
        if (storageType == null)
            return null;
        switch (storageType) {
            case FOLDER:
                return new FileVariableFolder().fromFolder(storageDefinition, value.toString());

            case TEMPFOLDER:
                return new FileVariableTempFolder().fromTempFolder(value.toString());

            case JSON:
            default:
                return new FileVariableJSON().fromJson(value.toString());
        }

    }

    /**
     * SetFileVariable
     *
     * @param storageDefinition storage Definition to pilot the way to save the value
     * @param fileVariableValue file Variable to save
     * @return the object (depends on the storageDefinition code)
     * @throws Exception if an error arrive
     */
    public Object setFileVariable(String storageDefinition, FileVariable fileVariableValue) throws Exception {
        if (fileVariableValue == null)
            return null;

        FileVariableStorage storageType = extractStorageType(storageDefinition);
        if (storageType == null)
            return null;

        switch (storageType) {
            case FOLDER:
                return new FileVariableFolder().toFolder(storageDefinition, fileVariableValue);

            case TEMPFOLDER:
                return new FileVariableTempFolder().toTempFolder(fileVariableValue);

            case JSON:
            default:
                return new FileVariableJSON().toJson(fileVariableValue);
        }
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
            logger.error("Vercors.FileVariableFactory: can't decode storageDefinition [" + storageDefinition + "]. Format should be "
                    + FileVariableStorage.JSON + "|" + FileVariableStorage.TEMPFOLDER);
            return null;
        }
    }

    /**
     * Define how the file variable is stored.
     * JSON: easy, but attention to large file
     */
    public enum FileVariableStorage {JSON, TEMPFOLDER, FOLDER}
}
