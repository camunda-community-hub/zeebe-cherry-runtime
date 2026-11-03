/* ******************************************************************** */
/*                                                                      */
/*  GitHubRelease                                                       */
/*                                                                      */
/* Manage release access in a Github repository                         */
/*                                                                      */
/* ******************************************************************** */

package io.camunda.cherry.store.git;

import com.fasterxml.jackson.databind.JsonNode;
import io.camunda.cherry.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class GitHubRelease {
    Logger logger = LoggerFactory.getLogger(GitHubRelease.class.getName());
    private static final Pattern OFFICIAL_RELEASE = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    private String repoPath;
    private GitHubAccess gitHubAccess;

    protected GitHubRelease(String repoPath, GitHubAccess gitHubAccess) {
        this.repoPath = normalizeRepo(repoPath);
        this.gitHubAccess = gitHubAccess;
    }

    /**
     * Accepts "owner/repo", a github.com page URL, or an api.github.com/repos/... URL
     * (e.g. the repoPath used by StoreGithub, which ends with "/contents"), and reduces
     * it to the canonical "owner/repo" form used to build every GitHub API URL below.
     */
    private static String normalizeRepo(String gitRepository) {
        if (gitRepository == null) return null;
        return gitRepository
                .replaceFirst("^https?://github\\.com/", "")
                .replaceFirst("^https?://api\\.github\\.com/repos/", "")
                .replaceFirst("/contents.*$", "")
                .replaceFirst("/+$", "");
    }

    public JsonNode getAllRelease() {
        String releasesUrl = repoPath.replaceAll("(https://api\\.github\\.com/repos/[^/]+/[^/]+)/.*", "$1") + "/releases?per_page=1";
        JsonNode releases = null;
        try {
            return gitHubAccess.gitHubToolbox.getJsonNode(releasesUrl);
        } catch (Exception e) {
            logger.error("getLatestRelease repo[{}] urlToGetRelease[{}] : {}", repoPath, releasesUrl, e);
            return null;
        }
    }

    public String getLatestRelease() {
        if (repoPath == null || repoPath.isEmpty())
            return null;

        // Prefer GitHub's own "latest" release - it excludes drafts/prereleases and honors
        // a maintainer's manual "Latest" marking. Fall back to scanning all releases below
        // only when the repo has none (e.g. everything is a prerelease).
        String latestUrl = "https://api.github.com/repos/" + repoPath + "/releases/latest";
        try {
            JsonNode latest = gitHubAccess.gitHubToolbox.getJsonNode(latestUrl);
            String tagName = latest.path("tag_name").asText(null);
            if (tagName != null && !tagName.isBlank())
                return tagName;
        } catch (Exception e) {
            if (e.getMessage() == null || !e.getMessage().contains("404 Not Found")) {
                logger.error("getLatestRelease repo[{}] urlToGetRelease[{}] : {}", repoPath, latestUrl, e);
            }
        }

        String url = null;
        try {
            url = "https://api.github.com/repos/" + repoPath + "/releases?per_page=1000";

            JsonNode releases = JsonUtils.toJsonNode(gitHubAccess.gitHubToolbox.get(url));
            if (!releases.isArray()) return null;

            List<JsonNode> releaseList = new ArrayList<>();
            releases.forEach(releaseList::add);

            String release = releaseList.stream()
                    .filter(r -> OFFICIAL_RELEASE.matcher(r.path("tag_name").asText("")).matches())
                    .max(Comparator.comparingLong(r -> versionWeight(r.path("tag_name").asText(""))))
                    .map(r -> r.path("tag_name").asText())
                    .orElse(null);
            // if no official release is find, then we keep the first release, according it is order by the inverse order of the date
            if ((release == null || release.isBlank()) && !releaseList.isEmpty()) {
                release = releaseList.get(0).path("tag_name").asText();
            }
            return release;
        } catch (Exception e) {
            // this acceptable: the repo does not contains any target, or the link point to a path INSIDE a repo
            if (e.getMessage().contains("404 Not Found"))
                return null;
            logger.error("getLastestRelease repo[{}] urlToGetRelease[{}] : {}", repoPath, url, e);
            return null;
        }
    }

    public String getJarFromRelease(String release) throws Exception {
        String releaseUrl = "https://api.github.com/repos/" + repoPath + "/releases/tags/" + release;
        JsonNode releaseNode = gitHubAccess.gitHubToolbox.getJsonNode(releaseUrl);
        JsonNode assets = releaseNode.path("assets");
        if (assets.isArray()) {
            String urlJarFile = null;
            for (JsonNode asset : assets) {
                String assetName = asset.path("name").asText("");

                if (assetName.endsWith(".jar")) {
                    urlJarFile = asset.path("browser_download_url").asText();
                    // if the jar name ended with dependencies, we stop here immediately, because it is the one we want to use, otherwise we continue to search for a jar with dependencies
                    if (assetName.endsWith("-with-dependencies.jar"))
                        break;
                }
            }
            return urlJarFile;
        }
        return null;
    }

    private long versionWeight(String version) {
        String[] parts = version.split("\\.");
        long x = Long.parseLong(parts[0]);
        long y = Long.parseLong(parts[1]);
        long z = Long.parseLong(parts[2]);
        return x * 10_000_000L + y * 10_000L + z;
    }
}