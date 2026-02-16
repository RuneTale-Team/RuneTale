package org.runetale.skills.config;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public record HeuristicsConfig(
        @Nonnull List<String> nodeCandidateTokens) {

    private static final String RESOURCE_PATH = "Skills/Config/heuristics.properties";

    @Nonnull
    public static HeuristicsConfig load() {
        Properties properties = ConfigResourceLoader.loadProperties(RESOURCE_PATH);
        return new HeuristicsConfig(parseCsvLowercase(properties, "nodeCandidateTokens", List.of("log", "tree", "ore", "rock")));
    }

    private static List<String> parseCsvLowercase(
            @Nonnull Properties properties,
            @Nonnull String key,
            @Nonnull List<String> defaultValues) {
        String raw = properties.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return List.copyOf(defaultValues);
        }

        List<String> parsed = new ArrayList<>();
        for (String token : raw.split(",")) {
            String normalized = token.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                parsed.add(normalized);
            }
        }

        return parsed.isEmpty() ? List.copyOf(defaultValues) : List.copyOf(parsed);
    }
}
