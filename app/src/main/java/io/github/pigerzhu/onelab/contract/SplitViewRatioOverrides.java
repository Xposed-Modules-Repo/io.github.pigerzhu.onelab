package io.github.pigerzhu.onelab.contract;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Serialization shared by the configuration UI and the system_server ratio hook. */
public final class SplitViewRatioOverrides {
    private SplitViewRatioOverrides() {
    }

    public static Map<String, Float> parse(String raw) {
        LinkedHashMap<String, Float> values = new LinkedHashMap<>();
        if (raw == null || raw.trim().isEmpty()) return values;
        for (String entry : raw.split(";")) {
            String[] parts = entry.split(":");
            if (parts.length != 2 || parts[0].trim().isEmpty()) continue;
            try {
                float ratio = Float.parseFloat(parts[1].trim());
                if (ratio > 0f && ratio < 1f && Float.isFinite(ratio)) {
                    values.put(parts[0].trim(), ratio);
                }
            } catch (NumberFormatException ignored) {
                // Skip malformed entries without hiding the remaining application policies.
            }
        }
        return values;
    }

    public static Map<String, Float> immutableSnapshot(String raw) {
        return Collections.unmodifiableMap(parse(raw));
    }

    public static String serialize(Map<String, Float> values) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Float> entry : values.entrySet()) {
            float ratio = entry.getValue() == null ? 0f : entry.getValue();
            if (entry.getKey().isEmpty() || ratio <= 0f || ratio >= 1f
                    || !Float.isFinite(ratio)) {
                continue;
            }
            if (result.length() > 0) result.append(';');
            result.append(entry.getKey()).append(':')
                    .append(String.format(Locale.US, "%.6f", ratio));
        }
        return result.toString();
    }
}
