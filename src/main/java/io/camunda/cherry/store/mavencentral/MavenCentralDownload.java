package io.camunda.cherry.store.mavencentral;

import io.camunda.cherry.store.StoreAccess;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Download a connector's JAR - and all its dependency JARs - once its GitHub repository is known, by:
 * 1. Reading the connector's pom.xml from its GitHub repository to get groupId/artifactId/version and
 * its declared dependencies (parsed with Maven's own model reader, so a groupId/version inherited
 * from a &lt;parent&gt; resolves the same way Maven itself would resolve it).
 * 2. Looking for the built JAR on Maven Central first - the public repository, no credentials needed.
 * 3. Falling back to Camunda's own Artifactory (artifacts.camunda.com) for Camunda-internal artifacts
 * that never get published to Maven Central.
 * Every dependency declared in the pom.xml is downloaded from whichever repository the main JAR was
 * found on, and collected into ConnectorDownload.jarDependenciesContent, so the runtime does not need
 * a separate dependency-resolution step of its own.
 */
public class MavenCentralDownload {

    // The brief named "https://mvnrepository.com" for Maven Central, but that host is a browsing/search
    // site, not a repository - it has no predictable download URL for a given groupId/artifactId/version.
    // Used the actual Maven Central repository URL instead - the same one StoreCamundaConnector already
    // uses for this same kind of lookup (exploreJarUrl).
    private static final String MAVEN_CENTRAL_REPOSITORY = "https://repo.maven.apache.org/maven2";
    private static final String CAMUNDA_ARTIFACTORY_REPOSITORY = "https://artifacts.camunda.com/artifactory/connectors";
    private final RestTemplate restTemplate = new RestTemplate();
    Logger logger = LoggerFactory.getLogger(MavenCentralDownload.class.getName());

    public StoreAccess.ConnectorDownload downloadConnector(StoreAccess.ConnectorDefinition connectorDefinition) {
        // use connectorDefinition.url to access and read the pom.xml
        PomDescription pomDescription = readPom(connectorDefinition);
        if (pomDescription == null) {
            StoreAccess.ConnectorDownload connectorDownload = new StoreAccess.ConnectorDownload();
            connectorDownload.status = StoreAccess.STATUSDOWNLOAD.UNKNOWCONNECTOR;
            connectorDownload.explanation = "Can't read/parse the pom.xml for connector [" + connectorDefinition.name + "]";
            return connectorDownload;
        }

        // Now search in Maven Central for this item
        StoreAccess.ConnectorDownload connectorDownload = downloadFromRepository(MAVEN_CENTRAL_REPOSITORY, pomDescription);
        if (connectorDownload != null) {
            // Check the content to get all dependencies, and saved them under public List<ByteArrayInputStream> jarDependenciesContent;
            connectorDownload.jarDependenciesContent = downloadDependencies(MAVEN_CENTRAL_REPOSITORY, pomDescription);
            return connectorDownload;
        }

        // Now search in https://artifacts.camunda.com/ the same artefact
        connectorDownload = downloadFromRepository(CAMUNDA_ARTIFACTORY_REPOSITORY, pomDescription);
        if (connectorDownload != null) {
            // do the same for dependencies
            connectorDownload.jarDependenciesContent = downloadDependencies(CAMUNDA_ARTIFACTORY_REPOSITORY, pomDescription);
            return connectorDownload;
        }

        connectorDownload = new StoreAccess.ConnectorDownload();
        connectorDownload.status = StoreAccess.STATUSDOWNLOAD.UNKNOWNRELEASE;
        connectorDownload.explanation = "Artifact [" + pomDescription.groupId + ":" + pomDescription.artifactId + ":"
                + pomDescription.version + "] not found on Maven Central or on Camunda's Artifactory";
        return connectorDownload;
    }

    /**
     * Read the connector's pom.xml from its GitHub repository (the public, unauthenticated raw-content
     * URL - same approach StoreCamundaConnector already uses for the same need) and parse it with
     * Maven's own model reader, so a groupId/version inherited from a &lt;parent&gt; is resolved the same
     * way Maven itself would resolve it, instead of hand-rolling that inheritance with XPath.
     *
     * @param connectorDefinition connector to read the pom.xml from
     * @return the groupId/artifactId/version/dependencies found, or null if the pom.xml could not be
     * read or parsed, or is missing coordinates
     */
    private PomDescription readPom(StoreAccess.ConnectorDefinition connectorDefinition) {
        if (connectorDefinition.githubRepoName == null) {
            logger.error("MavenCentralDownload: connector [{}] has no githubRepoName, can't locate its pom.xml",
                    connectorDefinition.name);
            return null;
        }
        String githubRepoPath = connectorDefinition.githubRepoPath == null ? "" : connectorDefinition.githubRepoPath;
        String release = connectorDefinition.release == null ? "main" : connectorDefinition.release;
        String url = "https://raw.githubusercontent.com/" + connectorDefinition.githubRepoName + "/" + release
                + "/" + (githubRepoPath.isEmpty() ? "" : githubRepoPath + "/") + "pom.xml";

        try {
            String pomContent = restTemplate.getForObject(url, String.class);
            if (pomContent == null)
                return null;

            Model model = new MavenXpp3Reader().read(new StringReader(pomContent));

            PomDescription pomDescription = new PomDescription();
            pomDescription.groupId = model.getGroupId();
            pomDescription.artifactId = model.getArtifactId();
            pomDescription.version = model.getVersion();
            // groupId/version are often declared on the <parent> only (a multi-module reactor pom) -
            // fall back to it, the same way Maven itself resolves an inherited coordinate.
            if (model.getParent() != null) {
                if (pomDescription.groupId == null)
                    pomDescription.groupId = model.getParent().getGroupId();
                if (pomDescription.version == null)
                    pomDescription.version = model.getParent().getVersion();
            }
            if (pomDescription.groupId == null || pomDescription.artifactId == null || pomDescription.version == null) {
                logger.error("MavenCentralDownload: pom.xml at [{}] is missing groupId/artifactId/version", url);
                return null;
            }
            pomDescription.dependencies = model.getDependencies() == null ? List.of() : model.getDependencies();
            return pomDescription;
        } catch (Exception e) {
            logger.error("MavenCentralDownload: can't read/parse pom.xml at [{}]: {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Download the connector's own JAR from the given Maven repository. A cheap HEAD probe (same
     * pattern as StoreCamundaConnector.jarExists) is done first, trying the "-with-dependencies"
     * classifier before the plain jar, since some published artifacts already bundle their
     * dependencies - in which case downloadDependencies() below will simply find nothing left to add.
     *
     * @param repositoryUrl  base URL of the Maven repository to search (Maven Central, or Camunda's Artifactory)
     * @param pomDescription groupId/artifactId/version to locate
     * @return a ConnectorDownload with the JAR content, or null if this repository doesn't have it
     */
    private StoreAccess.ConnectorDownload downloadFromRepository(String repositoryUrl, PomDescription pomDescription) {
        String artifactBase = artifactBaseUrl(repositoryUrl, pomDescription.groupId, pomDescription.artifactId,
                pomDescription.version);

        String jarUrl = jarExists(artifactBase + "-with-dependencies.jar")
                ? artifactBase + "-with-dependencies.jar"
                : artifactBase + ".jar";
        if (!jarExists(jarUrl))
            return null;

        byte[] jarBytes = downloadBytes(jarUrl);
        if (jarBytes == null)
            return null;

        StoreAccess.ConnectorDownload connectorDownload = new StoreAccess.ConnectorDownload();
        connectorDownload.status = StoreAccess.STATUSDOWNLOAD.OK;
        connectorDownload.jarName = pomDescription.artifactId + "-" + pomDescription.version + ".jar";
        connectorDownload.jarContent = new ByteArrayInputStream(jarBytes);
        connectorDownload.explanation = "Downloaded " + jarBytes.length + " bytes from [" + jarUrl + "]";
        return connectorDownload;
    }

    /**
     * Download every dependency declared in the pom.xml, from the same repository the main JAR was
     * found on. A dependency that can't be located (test/provided scope, or a version managed
     * elsewhere - e.g. a BOM - which a raw pom.xml parse can't resolve) is skipped rather than failing
     * the whole download: the connector's own JAR is what matters most.
     */
    private List<ByteArrayInputStream> downloadDependencies(String repositoryUrl, PomDescription pomDescription) {
        List<ByteArrayInputStream> jarDependenciesContent = new ArrayList<>();
        for (Dependency dependency : pomDescription.dependencies) {
            if ("test".equals(dependency.getScope()) || "provided".equals(dependency.getScope()))
                continue;
            if (dependency.getGroupId() == null || dependency.getVersion() == null)
                continue;

            String jarUrl = artifactBaseUrl(repositoryUrl, dependency.getGroupId(), dependency.getArtifactId(),
                    dependency.getVersion()) + ".jar";
            byte[] jarBytes = downloadBytes(jarUrl);
            if (jarBytes != null)
                jarDependenciesContent.add(new ByteArrayInputStream(jarBytes));
        }
        return jarDependenciesContent;
    }

    private String artifactBaseUrl(String repositoryUrl, String groupId, String artifactId, String version) {
        return repositoryUrl + "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId
                + "-" + version;
    }

    private byte[] downloadBytes(String url) {
        try {
            return restTemplate.getForObject(url, byte[].class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Cheap HEAD probe to check a jar exists before attempting a full download - same pattern as
     * StoreCamundaConnector.jarExists.
     */
    private boolean jarExists(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int code = connection.getResponseCode();
            connection.disconnect();
            return code == 200;
        } catch (IOException e) {
            return false;
        }
    }

    private static class PomDescription {
        public String groupId;
        public String artifactId;
        public String version;
        public List<Dependency> dependencies = List.of();
    }
}
