/* ******************************************************************** */
/*                                                                      */
/*  StoreService                                                        */
/*                                                                      */
/*  Access the Store Service - download new connector                   */
/* ******************************************************************** */
package io.camunda.cherry.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.util.JsonUtils;
import io.camunda.connector.api.annotation.OutboundConnector;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class StoreService {

  public static final String REPO = "camunda/connectors-bundle";
  public static final List<String> IGNORE = List.of("github");

  private final Map<String, Map<String, String>> releaseConnectors = new HashMap<>();

  private final RestTemplate restTemplate = new RestTemplate();

  public String getLatestRelease() {
    JsonNode response = get("https://api.github.com/repos/" + REPO + "/releases/latest");
    return response.get("name").asText();
  }

  private JsonNode get(String url) throws TechnicalException {
    try {
      return JsonUtils.toJsonNode(restTemplate.getForObject(url, String.class));
    } catch (RestClientException | IOException e) {
      throw new TechnicalException("ControllerPage reading " + url + " : " + e.getMessage(), e);
    }
  }

  private String getConnectorsUrl(String release) {
    JsonNode tree = get("https://api.github.com/repos/" + REPO + "/git/trees/" + release);
    JsonNode subtrees = tree.get("tree");
    for (JsonNode subtree : subtrees) {
      if ("connectors".equals(subtree.get("path").asText())) {
        return subtree.get("url").asText();
      }
    }
    return null;
  }

  /**
   * @param release release
   * @return list of connector
   */
  public Map<String, String> listConnectors(String release) {
    String connectorsUrl = getConnectorsUrl(release);
    JsonNode tree = get(connectorsUrl);
    JsonNode subtrees = tree.get("tree");
    Map<String, String> connectorUrls = new HashMap<>();
    for (JsonNode subtree : subtrees) {
      if ("tree".equals(subtree.get("type").asText()) && !IGNORE.contains(subtree.get("path").asText())) {
        connectorUrls.put(subtree.get("path").asText(), subtree.get("url").asText());
      }
    }
    releaseConnectors.put(release, connectorUrls);
    return connectorUrls;
  }

  public ConnectorStore downloadConnector(String name, String release) {
    String mavenJarUrl = getMavenCentralUrl(release, name);
    String jarName = mavenJarUrl.substring(mavenJarUrl.lastIndexOf("/") + 1);
    try {
      ConnectorStore connectorStore = new ConnectorStore();
      URL url = new URL(mavenJarUrl);
      URLConnection connection = url.openConnection();
      InputStream is = connection.getInputStream();

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      IOUtils.copy(is, byteArrayOutputStream);
      connectorStore.jarContent = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

      Path tempPath = Files.createTempFile(name + "-" + release, ".jar");
      File tempFile = new File(tempPath.toString());
      FileOutputStream tempOut = new FileOutputStream(tempFile);
      IOUtils.copy(connectorStore.jarContent, tempOut);
      connectorStore.listConnectors = fetchDetails(tempFile);
      tempFile.delete();
      // get the element Template now
      JsonNode jsonNode = getElementTemplate(name, release);
      connectorStore.elementTemplate = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);

      return connectorStore;

    } catch (IOException e) {
      throw new TechnicalException("ControllerPage downloading " + mavenJarUrl, e);
    }
  }

  public List<ConnectorDetail> fetchDetails(File connectorFile) throws TechnicalException {
    List<ConnectorDetail> listConnectorDetails = new ArrayList<>();
    try {
      ZipFile jarFile = new ZipFile(connectorFile);
      Enumeration<? extends ZipEntry> entries = jarFile.entries();

      URLClassLoader loader = new URLClassLoader(new URL[] { connectorFile.toURI().toURL() });

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
  private JsonNode getElementTemplate(String name, String release) {
    String connectorUrl = releaseConnectors.get(release).get(name);
    JsonNode tree = get(connectorUrl);
    JsonNode subtrees = tree.get("tree");
    for (JsonNode subtree : subtrees) {
      if ("element-templates".equals(subtree.get("path").asText())) {
        String elementTemplateUrl = subtree.get("url").asText();
        JsonNode elementTemplateTree = get(elementTemplateUrl);
        JsonNode elementTemplateSubtrees = elementTemplateTree.get("tree");
        for (JsonNode jsonFile : elementTemplateSubtrees) {
          String jsonEltTemplateRawRul =
              "https://raw.githubusercontent.com/" + REPO + "/" + release + "/connectors/" + name
                  + "/element-templates/" + jsonFile.get("path").asText();
          return get(jsonEltTemplateRawRul);
        }
      }
    }
    return null;
  }

  /**
   * private void getVariablesAndJobType(Connector connector, String name, String release) {
   * connector.setFetchVariables(new ArrayList<>()); JsonNode template = getElementTemplate(name,
   * release); JsonNode properties = template.get("properties"); for (JsonNode prop : properties) {
   * JsonNode binding = prop.get("binding"); if
   * ("zeebe:taskDefinition:type".equals(binding.get("type").asText())) {
   * connector.setJobType(prop.get("value").asText()); } else if
   * ("zeebe:input".equals(binding.get("type").asText())) { String input =
   * binding.get("name").asText(); String variable = input; if (input.indexOf(".") > 0) { variable =
   * input.substring(0, input.indexOf(".")); } if
   * (!connector.getFetchVariables().contains(variable)) {
   * connector.getFetchVariables().add(variable); } } } }
   */
  private String getMavenCentralUrl(String release, String name) {
    String groupId = "";
    String artifactId = "";
    try {
      String pom = restTemplate.getForObject(
          "https://raw.githubusercontent.com/" + REPO + "/" + release + "/connectors/" + name + "/pom.xml",
          String.class);
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(new ByteArrayInputStream(pom.getBytes()));
      XPath xPath = XPathFactory.newInstance().newXPath();
      NodeList nodes = (NodeList) xPath.evaluate("/project/parent/groupId", doc, XPathConstants.NODESET);
      for (int i = 0; i < nodes.getLength(); ++i) {
        groupId = nodes.item(i).getTextContent();
      }
      nodes = (NodeList) xPath.evaluate("/project/artifactId", doc, XPathConstants.NODESET);
      for (int i = 0; i < nodes.getLength(); ++i) {
        artifactId = nodes.item(i).getTextContent();
      }
      return "https://repo.maven.apache.org/maven2/" + groupId.replaceAll("\\.", "/") + "/" + artifactId + "/" + release
          + "/" + artifactId + "-" + release + "-with-dependencies.jar";
    } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
      throw new TechnicalException("ControllerPage building the maven url from the pom", e);
    } catch (Exception ex) {
      throw new TechnicalException("Can't access the repository", ex);
    }
  }

  /**
   * public Connector getConnector(String name, String release) { try { Connector connector = new
   * Connector(); connector.setJarFile(downloadMavenJar(release, name));
   * connectorStorageService.fetchDetails(connector); connector.setName(name + "-" + release); if
   * (connector.getFetchVariables() == null || connector.getJobType() == null) {
   * getVariablesAndJobType(connector, name, release); } return connector; } catch (Exception e) {
   * return null; } }
   */
  public class ConnectorStore {
    public String elementTemplate;
    ByteArrayInputStream jarContent;
    List<ConnectorDetail> listConnectors;
  }

  public class ConnectorDetail {
    public String className;
    public List<String> fetchVariables;
    public String name;
    public String type;
  }
}
