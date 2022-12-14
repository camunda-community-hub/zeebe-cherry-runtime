/* ******************************************************************** */
/*                                                                      */
/*  FileVariableCMIS                                                    */
/*                                                                      */
/*  Save a file variable in CMIS,                                       */
/* ******************************************************************** */
package io.camunda.file.storage;

import com.google.gson.Gson;
import io.camunda.file.storage.cmis.CmisConnection;
import io.camunda.file.storage.cmis.CmisFactoryConnection;
import io.camunda.file.storage.cmis.CmisParameters;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

public class StorageCMIS extends Storage {
    Logger logger = LoggerFactory.getLogger(StorageCMIS.class.getName());


    protected StorageCMIS(StorageDefinition storageDefinition,  FileRepoFactory fileRepoFactory) {
        super(storageDefinition, fileRepoFactory);
    }

    @Override
    public String getName() {
        return "CMIS";
    }

    /**
     *
     * @param url Url to connect the CMIS repository
     * @param repositoryName name of the repository. If null, then use "default"
     * @param userName user name to connect
     * @param password password to connect
     * @param storageDefinitionFolder folder in the CMIS to store the default repository. If null, default is "storagecmis"
     * @return
     */
    public static String getStorageDefinitionString( String url, String repositoryName, String userName, String password, String storageDefinitionFolder ) {
        CmisParameters cmisParameters = new CmisParameters();
        cmisParameters.url = url;
        cmisParameters.repositoryName = repositoryName;
        if (cmisParameters.repositoryName == null)
            cmisParameters.repositoryName = "default";
        cmisParameters.userName = userName;
        cmisParameters.password = password;
        cmisParameters.storageDefinitionFolder = storageDefinitionFolder;
        if (cmisParameters.storageDefinitionFolder==null)
            cmisParameters.storageDefinitionFolder="storagecmis";

        Gson gson = new Gson();
        String complement = gson.toJson(cmisParameters);
        return StorageDefinition.StorageDefinitionType.CMIS.toString() + StorageDefinition.STORAGE_DEFINITION_DELIMITATEUR + complement;
    }
    /**
     * Save the file Variable structure in the CMIS repository
     *
     * @param fileVariable      fileVariable to save it
     * @param fileVariableReference  file variable to update (may be null)
     */
    public FileVariableReference toStorage( FileVariable fileVariable, FileVariableReference fileVariableReference) throws Exception {
        CmisParameters cmisParameters = CmisParameters.getCodingConnection(getStorageDefinition().complementInObject);
        CmisConnection cmisConnection = CmisFactoryConnection.getInstance().getCmisConnection(cmisParameters);
        if (cmisConnection == null)
            throw new Exception("Can't connect the the CMIS repository");
        ByteArrayInputStream documentValue = new ByteArrayInputStream(fileVariable.getValue());
        // Add a random timestamp on the document name

        String uniqId = getFileRepoFactory().generateUniqId();
        Folder folder = cmisConnection.getFolderByPath(cmisParameters.storageDefinitionFolder);
        if (folder == null)
            throw new Exception("Folder [" + cmisParameters.storageDefinitionFolder + "] does not exists");

        cmisConnection.uploadNewDocument(
                CmisConnection.DocumentProperties.getDocument(cmisParameters.storageDefinitionFolder,fileVariable.getName() + uniqId),
                documentValue,
                fileVariable.getValue().length,
                fileVariable.getMimeType());

        FileVariableReference fileVariableReferenceOutput = new FileVariableReference();
        fileVariableReferenceOutput.storageDefinition = getStorageDefinition().encodeToString();
        fileVariableReferenceOutput.content = fileVariable.getName() + uniqId;
        return fileVariableReferenceOutput;

    }

    /**
     * read the fileVariable
     *
     * @param fileVariableReference          name of the file in the temporary directory
     * @return the fileVariable object
     * @throws Exception during the writing
     */
    public FileVariable fromStorage( FileVariableReference fileVariableReference) throws Exception {
        CmisParameters cmisParameters = CmisParameters.getCodingConnection(getStorageDefinition().complementInObject);
        CmisConnection cmisConnection = CmisFactoryConnection.getInstance().getCmisConnection(cmisParameters);
        if (cmisConnection == null)
            throw new Exception("Can't connect the the CMIS repository");
        ContentStream documentStream = cmisConnection.getDocumentByPath(cmisParameters.storageDefinitionFolder, fileVariableReference.content.toString());
        FileVariable fileVariable = new FileVariable(getStorageDefinition());
        fileVariable.setName( fileVariableReference.content.toString() );
        fileVariable.setMimeType( documentStream.getMimeType());
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[4000];

            while ((nRead = documentStream.getStream().read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            fileVariable.setValue( buffer.toByteArray());
            return fileVariable;
        } catch (Exception e) {
            logger.error(getFileRepoFactory().getLoggerHeaderMessage(StorageCMIS.class)+": exception " + e + " During read file[" + fileVariableReference.content.toString() + "]");
            throw e;
        }
    }


    /**
     * Remove a file in the directory
     * Remove a file in the directory
     * @param fileVariableReference          name of the file in the temporary directory
     * @return true if the operation was successful
     */
    public boolean purgeStorage( FileVariableReference fileVariableReference)  throws Exception {
        CmisParameters cmisParameters = CmisParameters.getCodingConnection(getStorageDefinition().complementInObject);
        CmisConnection cmisConnection = CmisFactoryConnection.getInstance().getCmisConnection(cmisParameters);
        if (cmisConnection == null)
            throw new Exception("Can't connect the the CMIS repository");
        return cmisConnection.removeDocumentByPath(cmisParameters.storageDefinitionFolder, fileVariableReference.content.toString());
    }

}
