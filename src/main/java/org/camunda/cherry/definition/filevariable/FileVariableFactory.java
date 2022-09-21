/* ******************************************************************** */
/*                                                                      */
/*  FileVariableFactory                                                 */
/*                                                                      */
/*  File can't be saved in C8. So, different implementation to store    */
/*  files are possible, and the factory give access to the different    */
/* formats                                                              */
/* ******************************************************************** */
package org.camunda.cherry.definition.filevariable;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class FileVariableFactory {

    private final Random rand = new Random();


    /**
     * get the FileVariable object from the different information
     * StorageDefinition is a string like
     * "JSON" : the value is a JSON information, to be unSerialize
     * "FOLDER:<path>", and the value is a file name in this directory.
     *
     * @param fileContainer information to access the file
     * @return a fileVariable
     * @throws Exception can't load the fileVariable
     */
    public FileVariable getFileVariable(FileVariableReference fileContainer) throws Exception {
        if (fileContainer == null || fileContainer.content == null)
            return null;

        StorageDefinition storageDefinition = StorageDefinition.getFromString(fileContainer.storageDefinition);

        return switch (storageDefinition.type) {
            case FOLDER -> new FileVariableFolder(this).fromFolder(storageDefinition, fileContainer.content.toString());
            case CMIS -> new FileVariableCMIS(this).fromCmis(storageDefinition, fileContainer.content.toString());
            case TEMPFOLDER -> new FileVariableTempFolder().fromTempFolder(fileContainer.content.toString());
            case JSON -> new FileVariableJSON().fromJson(fileContainer.content.toString());
        };

    }

    /**
     * SetFileVariable
     *
     * @param storageDefinition storage Definition to pilot the way to save the value
     * @param fileVariableValue file Variable to save
     * @return the FileContainer (depends on the storageDefinition code)
     * @throws Exception if an error arrive
     */
    public FileVariableReference setFileVariable(StorageDefinition storageDefinition, FileVariable fileVariableValue) throws Exception {
        if (fileVariableValue == null)
            return null;


        FileVariableReference fileContainer = new FileVariableReference();

        fileContainer.storageDefinition = storageDefinition.encodeToString();

        switch (storageDefinition.type) {
            case FOLDER:
                fileContainer.content = new FileVariableFolder(this).toFolder(storageDefinition, fileVariableValue);
                break;

            case CMIS:
                fileContainer.content = new FileVariableCMIS(this).toCmis(storageDefinition, fileVariableValue);
                break;

            case TEMPFOLDER:
                fileContainer.content = new FileVariableTempFolder().toTempFolder(fileVariableValue);
                break;

            case JSON:
                fileContainer.content = new FileVariableJSON().toJson(fileVariableValue);
                break;
        }
        return fileContainer;
    }


    public boolean purgeFileVariable(FileVariableReference fileContainer) throws Exception {
        if (fileContainer == null)
            return true;

        StorageDefinition storageDefinition = StorageDefinition.getFromString(fileContainer.storageDefinition);

        switch (storageDefinition.type) {
            case FOLDER:
                return new FileVariableFolder(this).removeFile(storageDefinition, fileContainer.content.toString());

            case CMIS:
                new FileVariableCMIS(this).removeFile(storageDefinition, fileContainer.content.toString());
                return true;

            case TEMPFOLDER:
                return new FileVariableTempFolder().removeFile(fileContainer.content.toString());

        }
        return true;
    }

    /**
     * Generate an uniq Identifier for class who search for one
     *
     * @return a uniq ID
     */
    public String generateUniqId() {
        // get an uniq identifier
        return "_" + System.currentTimeMillis() + "_" + rand.nextInt(10000);
    }
}
