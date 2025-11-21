package io.openwallet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MainAppTest {

    @Test
    void launchDoesNotThrow() {
        // JavaFX cannot run headless during unit tests, so just ensure main method exists.
        assertDoesNotThrow(() -> MainApp.main(new String[]{}));
    }
}
