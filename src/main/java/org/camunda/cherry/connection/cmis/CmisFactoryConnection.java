/* ******************************************************************** */
/*                                                                      */
/*  CmisFactoryConnection                                               */
/*                                                                      */
/*  Manage all connection for CMIS                                      */
/* ******************************************************************** */
package org.camunda.cherry.connection.cmis;


import java.util.HashMap;
import java.util.Map;

public class CmisFactoryConnection {

    public Map<String, CmisConnection> allConnections = new HashMap<>();

    private static CmisFactoryConnection singletonCmisFactory = new CmisFactoryConnection();

    public static CmisFactoryConnection getInstance() {
        return singletonCmisFactory;
    }

    public CmisConnection getCmisConnection(CmisParameters cmisParameters) {
        String signature = getSignature(cmisParameters);
        if (allConnections.containsKey(signature)) {
            return allConnections.get(signature);
        }
        synchronized (allConnections) {
            // check again: two threads maybe waiting in the synchronized
            if (allConnections.containsKey(signature))
                return allConnections.get(signature);
            CmisConnection cmisConnection = new CmisConnection(cmisParameters);
            cmisConnection.connect();
            allConnections.put(signature, cmisConnection);
            return cmisConnection;
        }
    }


    /**
     * Calculate a uniq signature for a CMIS repository
     *
     * @param cmisParameters Parameter to access the repository
     * @return a signature
     */
    public String getSignature(CmisParameters cmisParameters) {
        return cmisParameters.typeConnection.toString() + "#" + cmisParameters.url + "#" + cmisParameters.repositoryName + "#" + cmisParameters.userName;
    }
}
