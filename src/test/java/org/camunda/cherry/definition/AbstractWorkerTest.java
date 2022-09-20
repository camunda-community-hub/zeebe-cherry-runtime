package org.camunda.cherry.definition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractWorkerTest {

    @Test
    public void parseNumbersFromInputs() {

        assertFalse(AbstractWorker.canParse(Integer.class, "foo"));
        assertTrue(AbstractWorker.canParse(Integer.class, "01"));
        assertTrue(AbstractWorker.canParse(Integer.class, "1001"));
        assertTrue(AbstractWorker.canParse(Integer.class, 1001));

        assertTrue(AbstractWorker.canParse(Long.class, "1001"));
        assertTrue(AbstractWorker.canParse(Long.class, "1000000"));
        assertTrue(AbstractWorker.canParse(Long.class, 1000000));
    }
}
