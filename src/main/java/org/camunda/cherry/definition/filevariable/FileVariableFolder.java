/* ******************************************************************** */
/*                                                                      */
/*  FileVariableFolder                                                  */
/*                                                                      */
/*  Save a file variable in a folder. Folder is part of the storageDef  */
/* syntax is FOLDER:<Path>                                              */
/*  Attention, the folder must be accessible where the worker is running*/
/* ******************************************************************** */
package org.camunda.cherry.definition.filevariable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringTokenizer;

public class FileVariableFolder {
    Logger logger = LoggerFactory.getLogger(FileVariableFolder.class.getName());

    /**
     * Save the file Variable structure in the temporary folder
     *
     * @param storageDefinition storageDefinition to get the path
     * @param fileVariable      fileVariable to save it
     */
    public String toFolder(String storageDefinition, FileVariable fileVariable) throws Exception {
        Path tempPath = null;
        try {
            Path pathFolder = extractPath(storageDefinition);
            Path file = Paths.get(pathFolder + FileSystems.getDefault().getSeparator() + fileVariable.name);
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
    public FileVariable fromFolder(String storageDefinition, String fileName) throws Exception {
        Path pathFolder = null;
        try {
            pathFolder = extractPath(storageDefinition);

            FileVariable fileVariable = new FileVariable();
            fileVariable.name = fileName;
            fileVariable.mimeType = FileVariable.getMimeTypeFromName(fileName);
            fileVariable.value = Files.readAllBytes(Paths.get(pathFolder + FileSystems.getDefault().getSeparator() + fileName));
            return fileVariable;

        } catch (Exception e) {
            logger.error("Cherry.FileVariableTempFolder: exception " + e + " During read file[" + fileName + "] in temporaryPath[" + pathFolder + "]");
            throw e;
        }
    }

    /**
     * Extract the path from the storage definition
     * convention is FOLDER:<path>
     *
     * @param storageDefinition the storage definition
     * @return the folder path
     */
    private Path extractPath(String storageDefinition) {
        StringTokenizer st = new StringTokenizer(storageDefinition, ":");
        if (st.hasMoreTokens())
            st.nextToken();
        String path = st.hasMoreTokens() ? st.nextToken() : null;
        if (path == null)
            return null;
        return Paths.get(path);
    }

}
