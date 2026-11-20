package io.camunda.cherry.zeebe;

import io.camunda.cherry.exception.TechnicalException;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.JobState;
import io.camunda.client.api.search.response.Job;
import io.camunda.client.api.search.response.SearchResponse;
import io.camunda.client.api.search.response.SearchResponsePage;
import io.camunda.client.api.search.response.Tenant;
import io.camunda.client.spring.properties.CamundaClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This call is used to connect the Orchestration API. Every call goes through the CamundaClient, which
 * owns the connection, the authentication and the address resolution (no hand-made HTTP call here).
 */
@Component
public class OrchestrationAPI {
    private final CamundaClientProperties camundaClientProperties;
    private final CamundaClient camundaClient;
    Logger logger = LoggerFactory.getLogger(OrchestrationAPI.class.getName());

    public OrchestrationAPI(CamundaClientProperties camundaClientProperties, CamundaClient camundaClient) {
        this.camundaClientProperties = camundaClientProperties;
        this.camundaClient = camundaClient;
    }

    /**
     * Root cause of an exception, as text: a failed CamundaClient call is wrapped in a CompletionException
     * and a ClientException whose own messages hide the real problem (e.g. java.net.ConnectException).
     */
    private static String describe(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.toString();
    }

    public List<TenantInformation> getListTenants() throws RuntimeException {
        List<TenantInformation> listTenants = new ArrayList<>();
        final int pageSize = 1000;
        final int maxPages = 100;

        try {
            String afterCursor = null;
            for (int page = 0; page < maxPages; page++) {
                final String cursor = afterCursor;
                SearchResponse<Tenant> response = camundaClient.newTenantsSearchRequest()
                        .page(p -> {
                            p.limit(pageSize);
                            if (cursor != null) {
                                p.after(cursor);
                            }
                        })
                        .send()
                        .join();
                for (Tenant tenant : response.items()) {
                    TenantInformation tenantInformation = new TenantInformation();
                    tenantInformation.name = tenant.getName();
                    tenantInformation.tenantId = tenant.getTenantId();
                    tenantInformation.description = tenant.getDescription();
                    listTenants.add(tenantInformation);
                }
                String endCursor = response.page().endCursor();
                if (response.items().size() < pageSize || endCursor == null) {
                    break;
                }
                afterCursor = endCursor;
            }
            return listTenants;
        } catch (Exception e) {
            logger.error("getListTenants: failed: {}", describe(e));
            throw new RuntimeException("Can't access " + camundaClientProperties.getRestAddress()
                    + "/v2/tenants/search: " + describe(e), e);
        }
    }

    /**
     * Returns the number of jobs currently waiting (state=CREATED) for the given job type.
     * Uses the CamundaClient job search (POST /v2/jobs/search): same connection, authentication and
     * address handling as the rest of Cherry, state-based and not time-windowed, so it captures
     * jobs regardless of when the process instance started.
     * <p>
     * The server caps page.totalItems at 10000 (hasMoreTotalItems=true), so when the cap is hit the
     * count is obtained by walking the cursor pages.
     */
    public long getJobCount(String jobType) {
        final int pageSize = 1000;
        final int maxPages = 1000;
        String afterCursor = null;
        long counted = 0;
        try {
            for (int page = 0; page < maxPages; page++) {
                final String cursor = afterCursor;
                SearchResponse<Job> response = camundaClient.newJobSearchRequest()
                        .filter(f -> f.type(jobType).state(JobState.CREATED))
                        .sort(s -> s.jobKey().asc())
                        .page(p -> {
                            p.limit(pageSize);
                            if (cursor != null) {
                                p.after(cursor);
                            }
                        })
                        .send()
                        .join();
                SearchResponsePage pageInfo = response.page();

                if (page == 0 && !Boolean.TRUE.equals(pageInfo.hasMoreTotalItems())) {
                    // Not capped: totalItems is the exact count, no need to walk the pages.
                    return pageInfo.totalItems() == null ? 0 : pageInfo.totalItems();
                }

                counted += response.items().size();
                if (response.items().size() < pageSize || pageInfo.endCursor() == null) {
                    return counted;
                }
                afterCursor = pageInfo.endCursor();
            }
            logger.warn("getJobCount: jobType[{}] has more than {} jobs, count truncated", jobType, maxPages * pageSize);
            return counted;
        } catch (Exception e) {
            logger.error("getJobCount: jobType[{}] failed: {}", jobType, describe(e));
            throw new TechnicalException("Can't access " + camundaClientProperties.getRestAddress()
                    + "/v2/jobs/search: " + describe(e), e);
        }
    }

    /**
     * Returns the number of jobs currently waiting (state=CREATED) for every job type present on the
     * server, grouped by type. Uses the CamundaClient job search without a type filter, then tallies
     * the type of each returned job - one query instead of one getJobCount() call per runner.
     * <p>
     * Paginates through every page (cursor-based, sorted by the unique "jobKey") instead of reading
     * only the first page: a single unpaginated page cap previously let a low-volume job type's jobs
     * be silently excluded from the tally whenever other types' jobs filled up the page first.
     */
    public Map<String, Long> getAllJobCount() {
        Map<String, Long> result = new LinkedHashMap<>();
        final int pageSize = 1000;
        // Safety net against an unexpected non-advancing cursor: caps collection at 1,000 pages
        // (up to 1,000,000 jobs) instead of looping forever.
        final int maxPages = 1000;

        String afterCursor = null;
        try {
            for (int page = 0; page < maxPages; page++) {
                final String cursor = afterCursor;
                SearchResponse<Job> response = camundaClient.newJobSearchRequest()
                        .filter(f -> f.state(JobState.CREATED))
                        .sort(s -> s.jobKey().asc())
                        .page(p -> {
                            p.limit(pageSize);
                            if (cursor != null) {
                                p.after(cursor);
                            }
                        })
                        .send()
                        .join();
                for (Job job : response.items()) {
                    if (job.getType() != null) {
                        result.merge(job.getType(), 1L, Long::sum);
                    }
                }
                String endCursor = response.page().endCursor();
                // hasMoreTotalItems only says the total is capped, not that more pages exist: a short page is the end.
                if (response.items().size() < pageSize || endCursor == null) {
                    break;
                }
                afterCursor = endCursor;
            }
        } catch (Exception e) {
            logger.error("getAllJobCount: failed: {}", describe(e));
            return null;
        }
        return result;
    }

    public static class TenantInformation {
        public String tenantId;
        public String name;
        public String description;
        public boolean active = false;
    }


}
