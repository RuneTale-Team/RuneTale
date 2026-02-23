package org.runetale.skills.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record HeuristicsConfig(
        @Nonnull List<String> nodeCandidateTokens) {

    private static final String RESOURCE_PATH = "Skills/Config/gathering.json";

    @Nonnull
    public static HeuristicsConfig load(@Nonnull Path externalConfigRoot) {
        JsonObject root = ConfigResourceLoader.loadJsonObject(RESOURCE_PATH, externalConfigRoot);
        JsonObject heuristics = ConfigResourceLoader.objectValue(root, "heuristics");
        return new HeuristicsConfig(parseTokenList(heuristics, "nodeCandidateTokens", List.of("log", "tree", "ore", "rock")));
    }

    private static List<String> parseTokenList(
            @Nonnull JsonObject object,
            @Nonnull String key,
            @Nonnull List<String> defaultValues) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return List.copyOf(defaultValues);
        }

        List<String> parsed = new ArrayList<>();
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement tokenElement : array) {
                if (tokenElement == null || tokenElement.isJsonNull()) {
                    continue;
                }
                String normalized = tokenElement.getAsString().trim().toLowerCase(Locale.ROOT);
                if (!normalized.isEmpty()) {
                    parsed.add(normalized);
                }
            }
        } else {
            String raw = element.getAsString();
            for (String token : raw.split(",")) {
                String normalized = token.trim().toLowerCase(Locale.ROOT);
                if (!normalized.isEmpty()) {
                    parsed.add(normalized);
                }
            }
        }

        return parsed.isEmpty() ? List.copyOf(defaultValues) : List.copyOf(parsed);
    }
}
