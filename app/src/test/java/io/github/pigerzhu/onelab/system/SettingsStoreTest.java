package io.github.pigerzhu.onelab.system;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class SettingsStoreTest {
    @Test
    public void shellQuotePreservesStructuredValues() {
        assertEquals(
                "'{\"settings\":{\"item\":\"a;b\"}}'",
                SettingsStore.shellQuote("{\"settings\":{\"item\":\"a;b\"}}")
        );
    }

    @Test
    public void shellQuoteEscapesSingleQuotes() {
        assertEquals("'a'\\''b'", SettingsStore.shellQuote("a'b"));
    }
}
