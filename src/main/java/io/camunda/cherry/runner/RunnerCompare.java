package io.camunda.cherry.runner;

import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.store.StoreAccess;

public class RunnerCompare {

    public enum COMPARISON {ENTITY_OLD, EQUALS, ENTITY_NEW}

    public static COMPARISON compare(StoreAccess.ConnectorDefinition connectorDefinition, RunnerDefinitionEntity runnerEntity) {
        if (connectorDefinition.release != null && runnerEntity.release != null) {
            int cmp = compareSemanticVersion(connectorDefinition.release, runnerEntity.release);
            if (cmp > 0)
                return COMPARISON.ENTITY_OLD;
            if (cmp < 0)
                return COMPARISON.ENTITY_NEW;
            return COMPARISON.EQUALS;
        }
        if (connectorDefinition.version > 0 && runnerEntity.version > 0) {
            if (connectorDefinition.version > runnerEntity.version) return COMPARISON.ENTITY_OLD;
            if (connectorDefinition.version < runnerEntity.version) return COMPARISON.ENTITY_NEW;
            return COMPARISON.EQUALS;
        }
        return COMPARISON.EQUALS;
    }

    /**
     * Compare two semantic version strings (e.g. "3.1.5" vs "3.10.0").
     * Returns positive if a > b, negative if a < b, 0 if equal.
     */
    public static int compareSemanticVersion(String a, String b) {
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        int len = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < len; i++) {
            int segA = i < partsA.length ? parseSegment(partsA[i]) : 0;
            int segB = i < partsB.length ? parseSegment(partsB[i]) : 0;
            if (segA != segB)
                return Integer.compare(segA, segB);
        }
        return 0;
    }

    private static int parseSegment(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
