/* ******************************************************************** */
/*                                                                      */
/*  CmisParameters                                                      */
/*                                                                      */
/*  Transport parameters to connect to a CMIS repository                */
/* ******************************************************************** */
package io.camunda.cherry.connection.cmis;

import java.util.Map;

public class CmisParameters {

    public TypeConnection typeConnection;
    public String url;
    public String repositoryName;
    public String userName;
    public String password;
    public String storageDefinitionFolder;

    public static CmisParameters getBrowserConnection(final String url, final String repositoryName, final String username, final String password) {
        CmisParameters cmisParameters = new CmisParameters();
        cmisParameters.typeConnection = TypeConnection.BROWSER;
        cmisParameters.url = url;
        cmisParameters.repositoryName = repositoryName;
        cmisParameters.userName = username;
        cmisParameters.password = password;
        return cmisParameters;
    }

    /**
     * Expected format is a JSON content
     *
     * @param cmisDefinition information to access the CMIS repository
     * @return the CMIS Parameters
     */
    public static CmisParameters getCodingConnection(final Object cmisDefinition) throws Exception {

        CmisParameters cmisParameters = new CmisParameters();
        cmisParameters.typeConnection = TypeConnection.BROWSER;
        if (cmisDefinition == null)
            throw new Exception("No CMIS connection are provided");

        if (cmisDefinition instanceof Map cmisDefinitionMap) {
            cmisParameters.url = (String) cmisDefinitionMap.get("url");
            cmisParameters.repositoryName = (String) cmisDefinitionMap.get("repository");
            if (cmisParameters.repositoryName==null)
                cmisParameters.repositoryName="default";
            cmisParameters.userName = (String) cmisDefinitionMap.get("userName");
            cmisParameters.password = (String) cmisDefinitionMap.get("password");
            cmisParameters.storageDefinitionFolder = (String) cmisDefinitionMap.get("storageDefinitionFolder");
            return cmisParameters;
        } else
            throw new Exception("CMIS parameter must be a Map format ["+cmisDefinition.getClass().getName()+"]");
    }

    public static String getGsonTemplate() {
        return "{\"repository\":\"String\"}";
    }
    public enum TypeConnection {BROWSER, ATOMPUB}


}
