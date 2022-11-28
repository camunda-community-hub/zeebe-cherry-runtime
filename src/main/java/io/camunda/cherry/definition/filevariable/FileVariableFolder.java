/* ******************************************************************** */
/*                                                                      */
/*  FileVariableFolder                                                  */
/*                                                                      */
/*  Save a file variable in a folder. Folder is part of the storageDef  */
/* syntax is FOLDER:<Path>                                              */
/*  Attention, the folder must be accessible where the worker is running*/
/* ******************************************************************** */
package io.camunda.cherry.definition.filevariable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileVariableFolder {
    Logger logger = LoggerFactory.getLogger(FileVariableFolder.class.getName());

    private final FileVariableFactory fileVariableFactory;

    public FileVariableFolder(FileVariableFactory fileVariableFactory) {
        this.fileVariableFactory =fileVariableFactory;
    }
    /**
     * Save the file Variable structure in the temporary folder
     *
     * @param storageDefinition storageDefinition to get the path
     * @param fileVariable      fileVariable to save it
     */
    public String toFolder(StorageDefinition storageDefinition, FileVariable fileVariable) throws Exception {
        Path tempPath = null;
        try {
            String uniqId = fileVariableFactory.generateUniqId();
            Path pathFolder = extractPath(storageDefinition);
            Path file = Paths.get(pathFolder + FileSystems.getDefault().getSeparator() + fileVariable.name + uniqId);
            Files.write(file, fileVariable.value);
            return file.getFileName().toString();
        } catch (Exception e) {
            logger.error("Cherry.FileVariableFolder: exception " + e + " During write fileVariable on tempFolder[" + tempPath + "]");
            throw e;
        }
    }

    /**
     * read the fileVariable
     *
     * @param storageDefinition the storage definition
     * @param fileName          name of the file in the temporary directory
     * @return the fileVariable object
     * @throws Exception during the writing
     */
    public FileVariable fromFolder(StorageDefinition storageDefinition, String fileName) throws Exception {
        Path pathFolder = null;
        try {
            pathFolder = extractPath(storageDefinition);

            FileVariable fileVariable = new FileVariable();
            fileVariable.name = fileName;
            fileVariable.mimeType = FileVariable.getMimeTypeFromName(fileName);
            fileVariable.storageDefinition=storageDefinition;
            fileVariable.value = Files.readAllBytes(Paths.get(pathFolder + FileSystems.getDefault().getSeparator() + fileName));
            return fileVariable;

        } catch (Exception e) {
            logger.error("Cherry.FileVariableTempFolder: exception " + e + " During read file[" + fileName + "] in temporaryPath[" + pathFolder + "]");
            throw e;
        }
    }

    /**
     * Remove a file in the directory
     * @param storageDefinition storage definition used to remove the file
     * @param fileName file name to delete
     * @return true if the operation was successful
     */
    public boolean removeFile(StorageDefinition storageDefinition, String fileName) {
       Path pathFolder = extractPath(storageDefinition);

       File file = new File(pathFolder + FileSystems.getDefault().getSeparator()+fileName);
       if (file.exists())
         return file.delete();
       return true;
    }
    /**
     * Extract the path from the storage definition
     * convention is FOLDER:<path>
     *
     * @param storageDefinition the storage definition
     * @return the folder path
     */
    private Path extractPath(StorageDefinition storageDefinition) {
        return Paths.get(storageDefinition.complement);
    }

}
