/* ******************************************************************** */
/*                                                                      */
/*  StoreCamundaConnector                                                        */
/*                                                                      */
/*  Access the Store Service - download new connector                   */
/* ******************************************************************** */
package io.camunda.cherry.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.connector.api.annotation.OutboundConnector;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class StoreCamundaConnector implements StoreAccess {
    public static final String REPO = "camunda/connectors";
    public static final String CONTENTS_URL = "https://api.github.com/repos/" + REPO + "/contents/connectors";
    public static final List<String> IGNORE = List.of("README.md", "src", "test", "docs", ".github");
    private final List<ConnectorDefinition> connectors = new ArrayList<>();
    private final RestTemplate restTemplate;
    private final GitHubAccess gitHubAccess;
    Logger logger = LoggerFactory.getLogger(StoreCamundaConnector.class.getName());

    public StoreCamundaConnector(GitHubAccess gitHubAccess) {
        // No auth token — camunda/connectors is a public repo and SAML enforcement
        // causes 401 when a token is sent without org SSO authorization.
        this.gitHubAccess = gitHubAccess;
        restTemplate = new RestTemplate();
    }


    // No longer needed — contents API returns the list directly


    public String getName() {
        return "Camunda Connector";
    }

    public String getUrl() {
        return "https://github.com/" + REPO;
    }

    @Override
    public String getType() {
        return "CamundaConnector";
    }



    /* ******************************************************************** */
    /*                                                                      */
    /*  Exploration                                                         */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * @return list of connector
     */
    public List<ConnectorDefinition> getListConnectors() {
        long startTime = System.currentTimeMillis();
        String release = gitHubAccess.getLatestRelease(REPO);
        logger.info("{}: last official release is [{}]", getName(), release);

        List<ConnectorDefinition> result = new ArrayList<>();
        try {
            JsonNode items = gitHubAccess.getJsonNode(CONTENTS_URL + "?ref=" + release);
            if (!items.isArray()) {
                return result;
            }
            for (JsonNode item : items) {
                result.addAll(exploreConnectorsInGithub("connectors", release, item));
            }
            logger.info("Store[{}] found {} connectors with release in {} ms", getName(), result.size(), release, System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            logger.error("Store[{}] error {}", getName(), e);
            return result;
        }
    }

    private List<ConnectorDefinition> exploreConnectorsInGithub(String repoPath, String release, JsonNode item) {
        List<ConnectorDefinition> result = new ArrayList<>();
        String type = item.path("type").asText();
        String name = item.path("name").asText();
        logger.debug("exploreConnectorsInGithub: explore connectors [{}/{}]", repoPath, name);
        if (!"dir".equals(type) || IGNORE.contains(name)) {
            return result;
        }
        String itemRepoPath = repoPath + "/" + name;
        String htmlUrl = item.path("html_url").asText();

        // Check if element-templates subfolder exists — if so, this dir is a connector
        String apiUrl = "https://api.github.com/repos/" + REPO + "/contents/" + repoPath + "/" + name;
        GitHubAccess.GithubConnectorStatus githubConnectorStatus = gitHubAccess.isGithubConnector(apiUrl, release, true, false);
        if (githubConnectorStatus.elementTemplates && githubConnectorStatus.pomXml) {
            logger.info("Store[{}] Detect connector[{}] in url[{}]", getName(), name, htmlUrl);
            ConnectorDefinition connectorDefinition = ConnectorDefinition.getInstance(this, name, htmlUrl, release);
            connectorDefinition.githubRepoName = REPO;
            connectorDefinition.githubRepoPath = repoPath + "/" + name;
            connectorDefinition.sourceUrl = htmlUrl;
            connectorDefinition.creator = "Camunda";
            result.add(connectorDefinition);
            return result;
        }
        if (githubConnectorStatus.elementTemplates) {
            logger.info("Store[{}] Detect connector [{}] with element-template, but no code, in url [{}]", getName(), name, htmlUrl);
            ConnectorDefinition connectorDefinition = ConnectorDefinition.getInstance(this, name, htmlUrl, release);
            connectorDefinition.githubRepoName = REPO;
            connectorDefinition.githubRepoPath = repoPath + "/" + name;
            connectorDefinition.hasImplementation = false;

            result.add(connectorDefinition);

            // this is a connector without implementation, using another connector like Rest, so ignore it, no need to deepdive recursively
            return result;
        }
        // No element-templates here — recurse into subdirectories
        String contentsApiUrl = "https://api.github.com/repos/" + REPO + "/contents/" + itemRepoPath + "?ref=" + release;
        try {
            JsonNode children = gitHubAccess.getJsonNode(contentsApiUrl);
            if (children.isArray()) {
                for (JsonNode child : children) {
                    String nameChildren = child.path("name").asText();
                    String typeChildren = child.path("type").asText();
                    logger.debug("exploreConnectorsInGithub: explore child connectors [{}/{}]", itemRepoPath, nameChildren);

                    if ("dir".equals(typeChildren))
                        result.addAll(exploreConnectorsInGithub(itemRepoPath, release, child));
                }
            }
        } catch (Exception e) {
            logger.warn("{} Cannot explore [{}]: {}", getName(), itemRepoPath, e.getMessage());
        }
        return result;
    }


    @Override
    public boolean exploreDetails(ConnectorDefinition connectorDefinition) throws TechnicalException {
        if (connectorDefinition.hasImplementation) {
            connectorDefinition = fillJarDownload(connectorDefinition);
        }
        // Search the element template
        connectorDefinition = fillElementTemplate(connectorDefinition);
        return true;
    }


    private ConnectorDefinition fillElementTemplate(ConnectorDefinition connectorDefinition) {
        try {
            connectorDefinition.urlElementTemplate = exploreElementTemplate(connectorDefinition);

            for (String urlTemplate : connectorDefinition.urlElementTemplate) {
                JsonNode jsonNode = gitHubAccess.getJsonNode(urlTemplate);
                connectorDefinition.description = jsonNode.path("description").asText(connectorDefinition.description);
                connectorDefinition.documentationRef = jsonNode.path("documentationRef").asText(connectorDefinition.documentationRef);
                // An inbound connector does not have type, so explore all json to get the list of
                if (connectorDefinition.connectorType == null) {
                    connectorDefinition.connectorType = gitHubAccess.extractTaskDefinitionType(jsonNode);
                }
                JsonNode iconNode = jsonNode.path("icon");
                if (!iconNode.isMissingNode() && !iconNode.isNull()) {
                    connectorDefinition.icon = iconNode.path("contents").asText(iconNode.asText(""));
                }

            }
            return connectorDefinition;
        } catch(TechnicalException te) {
            throw te;
        } catch (Exception e) {
            logger.error("During fill ElementTemplate Store[{}] connector[{}] Url[{}] : {}",
                    getName(),
                    connectorDefinition.name,
                    connectorDefinition.url,
                    e);
            throw new TechnicalException("During fillElementTemplate Store[" + getName() + "] connector[" + connectorDefinition.name + "]", e);
        }
    }

    /**
     * Searching the properties
     * "properties": [
     * {
     * "type": "Hidden",
     * "value": "io.camunda:http-json:1",
     * "binding": {
     * "type": "zeebe:taskDefinition",
     * "property": "type"
     * }
     * },
     *
     * @param elementTemplate the elementTemplate
     * @return the type
     */
    private String extractTaskDefinitionType(JsonNode elementTemplate) {
        return gitHubAccess.extractTaskDefinitionType(elementTemplate);
    }

    private List<String> exploreElementTemplate(ConnectorDefinition connectorDefinition) throws TechnicalException {
        try {
            List<String> rawUrl = gitHubAccess.exploreElementTemplate(
                    connectorDefinition.githubRepoName,
                    connectorDefinition.githubRepoPath,
                    connectorDefinition.release);
            if (rawUrl.isEmpty()) {
                logger.error("Stpre[{}] No .json element-template found for connector [{}] github repo[{}] repoPath[{}] release[{}] ",
                        getName(),
                        connectorDefinition.name,
                        connectorDefinition.githubRepoName,
                        connectorDefinition.githubRepoPath,
                        connectorDefinition.release);
                throw new TechnicalException("No .json element-template found for connector ["
                        + connectorDefinition.name + "] github repo["+connectorDefinition.githubRepoName
                        +"] repoPath[" +connectorDefinition.githubRepoPath
                        + "] release [" + connectorDefinition.release + "]");
            }
            return rawUrl;
        } catch (TechnicalException e) {
            throw e;
        } catch (Exception e) {
            throw new TechnicalException("Element-template path does not exist for connector ["
                    + connectorDefinition.name + "] release [" + connectorDefinition.release + "]", e);
        }
    }

    /**
     * Fill JarDownload: explore the URL to get the pom.xml. Then, calculated from the content of the pom.xml the maven central url
     */
    private ConnectorDefinition fillJarDownload(ConnectorDefinition connectorDefinition) {
        String groupId = "";
        String artifactId = "";
        String version = "";
        // String url = "https://raw.githubusercontent.com/" + githubUrl REPO + "/" + connectorDefinition.release + "/connectors/" + connectorDefinition.name + "/pom.xml";
        String url = "https://raw.githubusercontent.com/" + connectorDefinition.githubRepoName + "/" + connectorDefinition.release + "/" + connectorDefinition.githubRepoPath + "/pom.xml";

        try {
            String pom = restTemplate.getForObject(url, String.class);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(pom.getBytes()));

            groupId = getXmlValue("/project/parent/groupId", doc);
            artifactId = getXmlValue("/project/artifactId", doc);
            version = getXmlValue("/project/parent/version", doc);
            connectorDefinition.release = version;
            connectorDefinition.urlMaven = "https://repo.maven.apache.org/maven2/" + groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + version;
            String mavenBase = connectorDefinition.urlMaven + "/" + artifactId + "-" + version;
            String urlWithDeps = mavenBase + "-with-dependencies.jar";
            String urlPlain = mavenBase + ".jar";
            if (jarExists(urlWithDeps))
                connectorDefinition.urlJarFile = urlWithDeps;
            else if (jarExists(urlPlain))
                connectorDefinition.urlJarFile = urlPlain;
            return connectorDefinition;

        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            throw new TechnicalException("Access repository name [" + connectorDefinition.name + "] via url [" + url + "]", e);
        } catch (Exception ex) {
            throw new TechnicalException("Can't access the repository [" + connectorDefinition.name + "] via Url [" + url + "]", ex);
        }
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  Download                                                            */
    /*                                                                      */
    /* ******************************************************************** */

    @Override
    public ConnectorDownload downloadConnector(ConnectorDefinition connectorDefinition) {
        try {
            String jarName = connectorDefinition.urlMaven.substring(connectorDefinition.urlMaven.lastIndexOf("/") + 1);
            ConnectorDownload connectorDownload = new ConnectorDownload();
            URL url = new URL(connectorDefinition.urlMaven);
            URLConnection connection = url.openConnection();
            InputStream is = connection.getInputStream();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(is, byteArrayOutputStream);
            connectorDownload.jarContent = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

            Path tempPath = Files.createTempFile(connectorDefinition.name + "-" + connectorDefinition.release, ".jar");
            File tempFile = new File(tempPath.toString());
            FileOutputStream tempOut = new FileOutputStream(tempFile);
            IOUtils.copy(connectorDownload.jarContent, tempOut);
            connectorDownload.connectorDetails = fetchDetails(tempFile);
            tempFile.delete();

            return connectorDownload;
        } catch (Exception e) {
            throw new TechnicalException("ControllerPage downloading " + connectorDefinition.urlMaven, e);
        }
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  Toolbox                                                             */
    /*                                                                      */
    /* ******************************************************************** */


    private boolean jarExists(String url) {
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new URL(url).openConnection();
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

    private String getXmlValue(String path, Document doc) throws XPathExpressionException {
        String value = null;
        XPath xPath = XPathFactory.newInstance().newXPath();

        NodeList nodes = (NodeList) xPath.evaluate(path, doc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); ++i) {
            value = nodes.item(i).getTextContent();
        }
        return value;
    }

    /**
     * public Connector getConnector(String name, String release) { try { Connector connector = new
     * Connector(); connector.setJarFile(downloadMavenJar(release, name));
     * connectorStorageService.fetchDetails(connector); connector.setName(name + "-" + release); if
     * (connector.getFetchVariables() == null || connector.getJobType() == null) {
     * getVariablesAndJobType(connector, name, release); } return connector; } catch (Exception e) {
     * return null; } }
     */


    private List<ConnectorDetail> fetchDetails(File connectorFile) throws TechnicalException {
        List<ConnectorDetail> listConnectorDetails = new ArrayList<>();
        try {
            ZipFile jarFile = new ZipFile(connectorFile);
            Enumeration<? extends ZipEntry> entries = jarFile.entries();

            URLClassLoader loader = new URLClassLoader(new URL[]{connectorFile.toURI().toURL()});

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName != null && entryName.endsWith(".class")) {
                    String className = entryName.replace(".class", "").replace('/', '.');
                    Class<?> clazz = loader.loadClass(className);
                    OutboundConnector connectorAnnotation = clazz.getAnnotation(OutboundConnector.class);
                    if (connectorAnnotation != null) {
                        ConnectorDetail connectorDetail = new ConnectorDetail();
                        listConnectorDetails.add(connectorDetail);
                        connectorDetail.className = className;
                        connectorDetail.fetchVariables = Lists.newArrayList(connectorAnnotation.inputVariables());
                        connectorDetail.name = connectorAnnotation.name();
                        connectorDetail.type = connectorAnnotation.type();
                    }
                }
            }
            jarFile.close();
            loader.close();
        } catch (IOException | ClassNotFoundException e) {

        }
        return listConnectorDetails;
    }

    /**
     * public void downloadElementTemplate(Connector connector, String name, String release) {
     * JsonNode elementTemplateTree = getElementTemplate(name, release); //
     * connectorStorageService.saveElementTemplate(connector, elementTemplateTree); }
     */
    private JsonNode getElementTemplate(ConnectorDefinition connectorDefinition) throws Exception {

        String connectorUrl = connectorDefinition.url;

        JsonNode tree = gitHubAccess.getJsonNode(connectorUrl);
        JsonNode subtrees = tree.get("tree");
        for (JsonNode subtree : subtrees) {
            if ("element-templates".equals(subtree.get("path").asText())) {
                String elementTemplateUrl = subtree.get("url").asText();
                JsonNode elementTemplateTree = gitHubAccess.getJsonNode(elementTemplateUrl);
                JsonNode elementTemplateSubtrees = elementTemplateTree.get("tree");
                for (JsonNode jsonFile : elementTemplateSubtrees) {
                    /*
                    String jsonEltTemplateRawurl =
                            "https://raw.githubusercontent.com/" + REPO + "/" + release + "/connectors/" + name
                                    + "/element-templates/" + jsonFile.get("path").asText();
                     */
                    String jsonEltTemplateRawurl =
                            "https://raw.githubusercontent.com/" + connectorDefinition.githubRepoName
                                    + "/" + connectorDefinition.release
                                    + "/" + connectorDefinition.githubRepoPath
                                    + "/element-templates/" + jsonFile.get("path").asText();


                    return gitHubAccess.getJsonNode(jsonEltTemplateRawurl);
                }
            }
        }
        return null;
    }

/*
    private boolean hasElementTemplates(String repoPath, String release) {
        String apiUrl = "https://api.github.com/repos/" + REPO + "/contents/" + repoPath + "/element-templates?ref=" + release;
        try {
            JsonNode items = gitHubAccess.getJsonNode(apiUrl);
            return items.isArray();
        } catch (TechnicalException e) {
            return false;
        }
    }
*/
}
