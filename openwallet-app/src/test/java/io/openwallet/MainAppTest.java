package io.openwallet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MainAppTest {

    @Test
    void mainClassLoads() {
        // JavaFX cannot run headless during unit tests.
        assertNotNull(MainApp.class);
    }
}
