/* ******************************************************************** */
/*                                                                      */
/*  CmisConnection                                                      */
/*                                                                      */
/*  Manage the connection to one CMIS repository                        */
/* ******************************************************************** */
package org.camunda.cherry.connection.cmis;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class CmisConnection {
    protected Session session;
    private final CmisParameters cmisParameters;
    private Repository repository;


    public CmisConnection(CmisParameters cmisParameters) {
        this.cmisParameters = cmisParameters;
    }

    /**
     * Connect to the repository according parameters
     * https://chemistry.apache.org/java/examples/example-create-session.html
     *
     * @return true if the session is correct, false else
     */
    public boolean connect() {
        if (session != null) {
            session.clear();
            session = null;
        }
        Map<String, String> parameter = new HashMap<>();

        // user credentials
        parameter.put(SessionParameter.USER, cmisParameters.userName);
        parameter.put(SessionParameter.PASSWORD, cmisParameters.password);

        // connection settings
        if (cmisParameters.typeConnection == CmisParameters.TypeConnection.BROWSER) {
            parameter.put(SessionParameter.BROWSER_URL, cmisParameters.url);
            parameter.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());
        }
        if (cmisParameters.typeConnection == CmisParameters.TypeConnection.ATOMPUB) {
            parameter.put(SessionParameter.ATOMPUB_URL, cmisParameters.url);
            parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        }
        if (cmisParameters.repositoryName != null)
            parameter.put(SessionParameter.REPOSITORY_ID, cmisParameters.repositoryName);
        final SessionFactory sessionFactory = SessionFactoryImpl.newInstance();

        session = sessionFactory.createSession(parameter);
        return session != null;
    }

    /**
     * Disconnect from the repository
     */
    public void disconnect() {
        if (session != null) {
            session.clear();
            session = null;
        }
    }


    /**
     * Get a folder object from its path
     *
     * @param path path to the folder
     * @return the folder object
     */
    public Folder getFolderByPath(final String path) {
        return (Folder) session.getObjectByPath(path);
    }

    /**
     * Check if an object exists
     *
     * @param objectPath path to the object to check
     * @return true if the object exists
     */
    public Boolean checkIfObjectExists(final String objectPath) {
        try {
            session.getObjectByPath(objectPath);
            return true;
        } catch (final CmisObjectNotFoundException e) {
            return false;
        }
    }


    public static class DocumentProperties {
        String parentFolder;
        String documentName;
        String objectType = "cmis:document";
        VersioningState versioningState = null;

        /**
         * Ask to save a document. To not ask for a versioning policyu
         *
         * @param parentFolder
         * @param documentName
         * @return
         */
        public static DocumentProperties getDocument(String parentFolder, String documentName) {
            DocumentProperties documentProperties = new DocumentProperties();
            documentProperties.parentFolder = parentFolder;
            documentProperties.documentName = documentName;
            documentProperties.objectType = "cmis:document";
            documentProperties.versioningState = VersioningState.NONE;
            return documentProperties;
        }

        public static DocumentProperties getVersionnableDocument(String parentFolder, String documentName, String objectType) {
            DocumentProperties documentProperties = new DocumentProperties();
            documentProperties.parentFolder = parentFolder;
            documentProperties.documentName = documentName;
            documentProperties.objectType = objectType;
            documentProperties.versioningState = VersioningState.MAJOR;
            return documentProperties;
        }
    }

    /**
     * Upload a new document. If the document is new, it is created, else a new version of the document is uploaded
     *
     * @param documentProperties Properties to saved the document
     * @param inputDocument      Content of the document
     * @param mimeType           Mime type of the document
     * @return
     */
    public Document uploadNewDocument(DocumentProperties documentProperties, InputStream inputDocument, long length, final String mimeType) throws Exception {
        final Folder folder = getFolderByPath(documentProperties.parentFolder);
        if (folder == null) {
            throw new IllegalArgumentException("No folder [" + documentProperties.parentFolder + "] exists in [" + cmisParameters.repositoryName + "]");
        }
        final ContentStream contentStream = new ContentStreamImpl(documentProperties.documentName, BigInteger.valueOf(length), mimeType, inputDocument);
        Document existingDocument;
        try {
            existingDocument = (Document) session.getObjectByPath(documentProperties.parentFolder + "/" + documentProperties.documentName);
        } catch (Exception e) {
            // if the document does not exist, throw an exception
            existingDocument = null;
        }
        if (existingDocument != null) {
            existingDocument.setContentStream(contentStream, true);
            return existingDocument;
        } else {
            final Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(PropertyIds.OBJECT_TYPE_ID, documentProperties.objectType);
            properties.put(PropertyIds.NAME, documentProperties.documentName);
            // determine the versioning strategy
            VersioningState version = determineVersionStrategy(documentProperties);
            return folder.createDocument(properties, contentStream, version);
        }
    }

    /**
     * Object may accept, or not, a version policy. According to the request in the documentProperties, return the best policy, else throw an exception
     *
     * @param documentProperties properties required
     * @return the best policy
     */
    private VersioningState determineVersionStrategy(DocumentProperties documentProperties) throws Exception {
        if (VersioningState.NONE.equals(documentProperties.versioningState))
            return VersioningState.NONE;
        // is the type support the versionning?
        ObjectType type = session.getTypeDefinition(documentProperties.objectType);
        if (type instanceof DocumentType documentType) {
            if (documentType.isVersionable()) {
                // everything is possible here.
                return documentProperties.versioningState == null ? VersioningState.MAJOR : documentProperties.versioningState;
            }
            // a version state is required, but not supported: throw an exception
            throw new Exception("Can't accept versioning state [" + documentProperties.versioningState + "]");
        }
        if (documentProperties.versioningState == null)
            return VersioningState.NONE;
        throw new Exception("Can't accept versioning state [" + documentProperties.versioningState + "]");
    }

    /**
     * Delete a document
     *
     * @param parentFolder folder where the document is
     * @param documentName name of the document
     */
    public void deleteDocumentByPath(String parentFolder, final String documentName) {
        Document existingDocument = (Document) session.getObjectByPath(parentFolder + "/" + documentName);
        if (existingDocument != null)
            existingDocument.delete();
    }

    /**
     * Return a document in a folder
     *
     * @param parentFolder folder where the document is
     * @param documentName name of the document
     * @return the content of the document. Caller is responsible to close the InputStream
     */
    public ContentStream getDocumentByPath(String parentFolder, final String documentName) {
        Document existingDocument = (Document) session.getObjectByPath(parentFolder + "/" + documentName);
        return existingDocument.getContentStream();
    }

    /**
     * Delete a document
     *
     * @param parentFolder folder where the parent is
     * @param documentName name of the document
     * @return true if the document is deteled, false else (document does not exist)
     */
    public boolean removeDocumentByPath(String parentFolder, final String documentName) {
        Document existingDocument = (Document) session.getObjectByPath(parentFolder + "/" + documentName);
        if (existingDocument != null)
            session.delete(existingDocument);
        return false;
    }
}
