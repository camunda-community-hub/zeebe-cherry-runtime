/* ******************************************************************** */
/*                                                                      */
/*  FileVariableCMIS                                                    */
/*                                                                      */
/*  Save a file variable in CMIS,                                       */
/* ******************************************************************** */
package io.camunda.cherry.definition.filevariable;

import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import io.camunda.cherry.connection.cmis.CmisConnection;
import io.camunda.cherry.connection.cmis.CmisFactoryConnection;
import io.camunda.cherry.connection.cmis.CmisParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


public class FileVariableCMIS {
    Logger logger = LoggerFactory.getLogger(FileVariableCMIS.class.getName());

    private final FileVariableFactory fileVariableFactory;

    public FileVariableCMIS(FileVariableFactory fileVariableFactory) {
        this.fileVariableFactory = fileVariableFactory;
    }

    /**
     * Save the file Variable structure in the CMIS repository
     *
     * @param storageDefinition storageDefinition to get the path
     * @param fileVariable      fileVariable to save it
     */
    public String toCmis(StorageDefinition storageDefinition, FileVariable fileVariable) throws Exception {
        CmisParameters cmisParameters = CmisParameters.getCodingConnection(storageDefinition.complementInObject);
        CmisConnection cmisConnection = CmisFactoryConnection.getInstance().getCmisConnection(cmisParameters);
        if (cmisConnection == null)
            throw new Exception("Can't connect the the CMIS repository");
        ByteArrayInputStream documentValue = new ByteArrayInputStream(fileVariable.value);
        // Add a random timestamp on the document name

        String uniqId = fileVariableFactory.generateUniqId();
        Folder folder = cmisConnection.getFolderByPath(cmisParameters.storageDefinitionFolder);
        if (folder == null)
            throw new Exception("Folder [" + cmisParameters.storageDefinitionFolder + "] does not exists");

        cmisConnection.uploadNewDocument(
                CmisConnection.DocumentProperties.getDocument(cmisParameters.storageDefinitionFolder,fileVariable.name + uniqId),
                documentValue,
                fileVariable.value.length,
                fileVariable.mimeType);
        return fileVariable.name + uniqId;
    }

    /**
     * read the fileVariable
     *
     * @param storageDefinition the storage definition
     * @param fileName          name of the file in the temporary directory
     * @return the fileVariable object
     * @throws Exception during the writing
     */
    public FileVariable fromCmis(StorageDefinition storageDefinition, String fileName) throws Exception {
        CmisParameters cmisParameters = CmisParameters.getCodingConnection(storageDefinition.complementInObject);
        CmisConnection cmisConnection = CmisFactoryConnection.getInstance().getCmisConnection(cmisParameters);
        if (cmisConnection == null)
            throw new Exception("Can't connect the the CMIS repository");
        ContentStream documentStream = cmisConnection.getDocumentByPath(cmisParameters.storageDefinitionFolder, fileName);
        FileVariable fileVariable = new FileVariable();
        fileVariable.name = fileName;
        fileVariable.mimeType = documentStream.getMimeType();
        fileVariable.storageDefinition = storageDefinition;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[4000];

            while ((nRead = documentStream.getStream().read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            fileVariable.value = buffer.toByteArray();
            return fileVariable;
        } catch (Exception e) {
            logger.error("Cherry.FileVariableCmis: exception " + e + " During read file[" + fileName + "]");
            throw e;
        }
    }


    /**
     * Remove a file in the directory
     * @param storageDefinition Storage Definition
     * @param fileName filename to delete
     * @return true is the operation was successful
     */
    public boolean removeFile(StorageDefinition storageDefinition, String fileName) throws Exception {
        CmisParameters cmisParameters = CmisParameters.getCodingConnection(storageDefinition.complementInObject);
        CmisConnection cmisConnection = CmisFactoryConnection.getInstance().getCmisConnection(cmisParameters);
        if (cmisConnection == null)
            throw new Exception("Can't connect the the CMIS repository");
        return cmisConnection.removeDocumentByPath(cmisParameters.storageDefinitionFolder, fileName);
    }

}
