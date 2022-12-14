/* ******************************************************************** */
/*                                                                      */
/*  FileVariableFolder                                                  */
/*                                                                      */
/*  Save a file variable in a folder. Folder is part of the storageDef  */
/* syntax is FOLDER:<Path>                                              */
/*  Attention, the folder must be accessible where the worker is running*/
/* ******************************************************************** */
package io.camunda.file.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StorageFolder extends Storage{
    Logger logger = LoggerFactory.getLogger(StorageFolder.class.getName());


    protected StorageFolder(StorageDefinition storageDefinition,  FileRepoFactory fileRepoFactory) {
        super(storageDefinition, fileRepoFactory);
    }

    @Override
    public String getName() {
        return "Folder";
    }

    /**
     *
     * @param folder folder to save the file
     * @return the connection string
     */
    public static String getStorageDefinitionString(String folder ) {
        return StorageDefinition.StorageDefinitionType.FOLDER.toString()+StorageDefinition.STORAGE_DEFINITION_DELIMITATEUR+folder;
    }

    /**
     * Save the file Variable structure in the temporary folder
     *
     * @param fileVariable      fileVariable to save it
     * @param fileVariableReference  file variable to update (may be null)
     */
    public FileVariableReference toStorage( FileVariable fileVariable, FileVariableReference fileVariableReference) throws Exception {
        Path tempPath = null;
        try {
            String uniqId = fileVariableReference==null? getFileRepoFactory().generateUniqId() : (String) fileVariableReference.content;
            Path pathFolder = extractPath(getStorageDefinition());
            Path file = Paths.get(pathFolder + FileSystems.getDefault().getSeparator() + fileVariable.getName() + uniqId);
            Files.write(file, fileVariable.getValue());
            FileVariableReference fileVariableReferenceOutput = new FileVariableReference();
            fileVariableReferenceOutput.storageDefinition = getStorageDefinition().encodeToString();
            fileVariableReferenceOutput.content = file.getFileName().toString();
            return fileVariableReferenceOutput;

        } catch (Exception e) {
            logger.error(getFileRepoFactory().getLoggerHeaderMessage(StorageFolder.class)+"Exception " + e + " During write fileVariable on tempFolder[" + tempPath + "]");
            throw e;
        }
    }

    /**
     * read the fileVariable
     *
     * @param fileVariableReference          name of the file in the temporary directory
     * @return the fileVariable object
     * @throws Exception during the writing
     */
    public FileVariable fromStorage( FileVariableReference fileVariableReference) throws Exception {
        Path pathFolder = null;
        String fileName = fileVariableReference.content.toString();
        try {
            pathFolder = extractPath(getStorageDefinition());

            FileVariable fileVariable = new FileVariable(getStorageDefinition());
            fileVariable.setName( fileName);
            fileVariable.setMimeType( FileVariable.getMimeTypeFromName(fileName));
            fileVariable.setValue( Files.readAllBytes(Paths.get(pathFolder + FileSystems.getDefault().getSeparator() + fileName)));
            return fileVariable;

        } catch (Exception e) {
            logger.error(getFileRepoFactory().getLoggerHeaderMessage(StorageFolder.class)+"Exception " + e + " During read file[" + fileName + "] in temporaryPath[" + pathFolder + "]");
            throw e;
        }
    }

    /**
     * Remove a file in the directory
     * @param fileVariableReference          name of the file in the temporary directory
     * @return true if the operation was successful
     */
    public boolean purgeStorage( FileVariableReference fileVariableReference) {
       Path pathFolder = extractPath(getStorageDefinition());
        String fileName = fileVariableReference.content.toString();

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
