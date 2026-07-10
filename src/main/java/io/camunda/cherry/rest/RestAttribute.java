package io.camunda.cherry.rest;

public class RestAttribute {

    // --- Common ---
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String STATUS = "status";
    public static final String ID = "id";
    public static final String URL = "url";
    public static final String DESCRIPTION = "description";
    public static final String ICON = "icon";
    public static final String TIMESTAMP = "timestamp";
    public static final String VERSION = "version";
    public static final String ERROR = "error";
    public static final String ERRORS = "errors";
    public static final String COMMENT = "comment";
    public static final String MESSAGE = "message";
    // --- Runner / Dashboard ---
    public static final String CONNECTOR_TYPE = "connectorType";
    public static final String CLASS_RUNNER = "classrunner";
    public static final String COLLECTION_NAME = "collectionName";
    public static final String COLLECTION_NAME_RUNNER = "collectionname";
    public static final String FRAMEWORK_RUNNER = "frameworkrunner";
    public static final String LOGO = "logo";
    public static final String ACTIVE = "active";
    public static final String STATISTIC = "statistic";
    public static final String PERFORMANCE = "performance";
    public static final String NB_OVER_THRESHOLD = "nboverthreshold";
    public static final String NB_EXEC = "nbexec";
    public static final String NB_FAIL = "nbfail";
    public static final String NB_RUNNERS = "nbRunners";
    public static final String DETAILS = "details";
    public static final String TOTAL_EXECUTIONS_SUCCEEDED = "totalExecutionsSucceeded";
    public static final String TOTAL_EXECUTIONS_FAILED = "totalExecutionsFailed";
    public static final String TOTAL_EXECUTIONS_BPMN_ERRORS = "totalExecutionsBpmnErrors";
    public static final String TOTAL_EXECUTIONS = "totalExecutions";
    // --- Execution / Operation log ---
    public static final String HOSTNAME = "hostname";
    public static final String RUNNER_TYPE = "runnerType";
    public static final String EXECUTION_TIME = "executionTime";
    public static final String EXECUTION_MS = "executionMs";
    public static final String DURATION_MS = "durationms";
    public static final String OPERATION = "operation";
    public static final String OPERATIONS = "operations";
    public static final String EXECUTIONS = "executions";
    public static final String TYPE_EXECUTOR = "typeExecutor";
    public static final String ERROR_CODE = "errorCode";
    public static final String ERROR_EXPLANATION = "errorExplanation";
    // --- Store / Connector ---
    public static final String STORE = "store";
    public static final String STORES = "stores";
    public static final String STORE_RELEASE = "storerelease";
    public static final String CURRENT_RELEASE = "currentrelease";
    public static final String GITHUB_REPO_NAME = "githubRepoName";
    public static final String GITHUB_REPO_PATH = "githubRepoPath";
    public static final String EXPLORATION_STATUS = "explorationStatus";
    public static final String DOCUMENTATION_REF = "documentationRef";
    public static final String URL_ELEMENT_TEMPLATE = "urlElementTemplate";
    public static final String URL_JAR_FILE = "urlJarFile";
    public static final String URL_MAVEN = "urlMaven";
    public static final String HAS_IMPLEMENTATION = "hasImplementation";
    public static final String CREATOR = "creator";
    // --- Secret / Env ---
    public static final String VALUE = "value";
    public static final String IS_SECRET = "issecret";
    // --- Content / Jar ---
    public static final String STORAGE_ENTITY_ID = "storageentityid";
    public static final String ACTIVE_RUNNER = "activeRunner";
    public static final String USED_BY = "usedby";
    public static final String LOADED_TIME = "loadedtime";
    public static final String RESULT_LOAD = "resultLoad";
    // --- Monitoring / Zeebe ---
    public static final String ZEEBE_KIND_CONNECTION = "zeebekindconnection";
    public static final String CLOUD_REGION = "cloudRegion";
    public static final String CLOUD_CLUSTER_ID = "cloudClusterID";
    public static final String CLOUD_CLIENT_ID = "cloudClientID";
    public static final String CLOUD_CLIENT_SECRET = "cloudClientSecret";
    public static final String GRPC_ADDRESS = "grpcAddress";
    public static final String REST_ADDRESS = "restAddress";
    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String AUTORIZATION_SERVER_URL = "AutorizationServerUrl";
    public static final String CLIENT_AUDIENCE = "clientAudience";
    public static final String TENANT_IDS = "tenantIds";
    public static final String MAX_JOBS_ACTIVE = "maxJobsActive";
    public static final String NB_THREADS = "nbThreads";
    public static final String DATASOURCE_PRODUCT_NAME = "datasourceProductName";
    public static final String DATASOURCE_URL = "datasourceUrl";
    public static final String DATASOURCE_USER_NAME = "datasourceUserName";
    // --- Tenant ---
    public static final String TENANTS = "tenants";
    public static final String DELAY_REFRESH = "delayRefresh";
    private RestAttribute() {
    }
}
