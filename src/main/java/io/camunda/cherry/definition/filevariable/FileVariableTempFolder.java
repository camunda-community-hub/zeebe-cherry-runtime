/* ******************************************************************** */
/*                                                                      */
/*  FileVariableTempFolder                                              */
/*                                                                      */
/*  Save a file variable in the temporary folder of the host            */
/*  Attention, this is the temporary folder where the worker is running */
/* ******************************************************************** */
package io.camunda.cherry.definition.filevariable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileVariableTempFolder {
    Logger logger = LoggerFactory.getLogger(FileVariableTempFolder.class.getName());

    private FileVariableFactory fileVariableFactory;

    public FileVariableTempFolder(FileVariableFactory fileVariableFactory) {
        this.fileVariableFactory =fileVariableFactory;
    }
    /**
     * Save the file Variable structure in the temporary folder
     *
     * @param fileVariable fileVariable to save it
     */
    public String toTempFolder(FileVariable fileVariable) throws Exception {
        Path tempPath = null;
        try {
            String fileName = fileVariable.name;
            String suffix = "";
            int lastDot = fileVariable.name.lastIndexOf(".");
            if (lastDot != -1) {
                fileName = fileVariable.name.substring(0, lastDot) + "_";
                suffix = fileVariable.name.substring(lastDot + 1);
            }

            tempPath = Files.createTempFile(fileName, "." + suffix);
            Files.write(tempPath, fileVariable.value);
            return tempPath.getFileName().toString();
        } catch (Exception e) {
            logger.error("Cherry.FileVariableTempFolder: exception " + e + " During write fileVariable on tempFolder[" + tempPath + "]");
            throw e;
        }
    }

    /**
     * read the fileVariable
     *
     * @param storageDefinition the storage definition
     * @param tempFileName name of the file in the temporary directory
     * @return the fileVariable object
     * @throws Exception during the reading
     */
    public FileVariable fromTempFolder(StorageDefinition storageDefinition, String tempFileName) throws Exception {
        String tempFilePath = null;
        try {
            // get the temporary path
            Path tempPath = Files.createTempFile("", "");
            String separator = FileSystems.getDefault().getSeparator();
            tempFilePath = tempPath.toString().substring(0, tempPath.toString().lastIndexOf(separator));

            FileVariable fileVariable = new FileVariable();
            fileVariable.name = tempFileName;
            fileVariable.mimeType = FileVariable.getMimeTypeFromName(tempFileName);
            fileVariable.value = Files.readAllBytes(Paths.get(tempFilePath + separator + tempFileName));
            fileVariable.storageDefinition = storageDefinition;
            return fileVariable;

        } catch (Exception e) {
            logger.error("Cherry.FileVariableTempFolder: exception " + e + " During read file[" + tempFileName + "] in temporaryPath[" + tempFileName + "]");
            throw e;
        }
    }

    /**
     * Delete the file
     * @param fileName file to delete
     * @return true if the file is correctly delete, or if the file does not exist
     */
    public boolean removeFile( String fileName) {
        String tmpDir = System.getProperty("java.io.tmpdir");
        File file = new File(tmpDir + FileSystems.getDefault().getSeparator() +fileName);
        if (file.exists())
            return file.delete();
        return true;
    }
}
