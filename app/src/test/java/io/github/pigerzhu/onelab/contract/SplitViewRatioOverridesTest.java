package io.github.pigerzhu.onelab.contract;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SplitViewRatioOverridesTest {
    @Test
    public void acceptsExtremePositiveRatio() {
        Map<String, Float> values = SplitViewRatioOverrides.parse(
                "com.example.app:0.01");

        assertEquals(0.01f, values.get("com.example.app"), 0.000001f);
    }

    @Test
    public void malformedItemsDoNotHideValidItems() {
        Map<String, Float> values = SplitViewRatioOverrides.parse(
                "broken;zero:0;com.first:0.25;overflow:1;com.second:0.75");

        assertEquals(2, values.size());
        assertEquals(0.25f, values.get("com.first"), 0.000001f);
        assertEquals(0.75f, values.get("com.second"), 0.000001f);
    }

    @Test
    public void serializationRoundTripsMultiplePackages() {
        Map<String, Float> original = new LinkedHashMap<>();
        original.put("com.first", 0.333333f);
        original.put("com.second", 0.9f);

        Map<String, Float> restored = SplitViewRatioOverrides.parse(
                SplitViewRatioOverrides.serialize(original));

        assertEquals(original.size(), restored.size());
        assertEquals(original.get("com.first"), restored.get("com.first"), 0.000001f);
        assertEquals(original.get("com.second"), restored.get("com.second"), 0.000001f);
        assertFalse(SplitViewRatioOverrides.serialize(original).endsWith(";"));
    }
}
