package org.runetale.skills.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.runetale.skills.domain.ToolTier;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ToolingConfig {

    private static final String RESOURCE_PATH = "Skills/Config/gathering.json";

    private final String defaultKeyword;
    private final Map<String, List<String>> familyFragmentsByKeyword;
    private final List<Map.Entry<ToolTier, List<String>>> orderedTierFragments;

    private ToolingConfig(
            @Nonnull String defaultKeyword,
            @Nonnull Map<String, List<String>> familyFragmentsByKeyword,
            @Nonnull List<Map.Entry<ToolTier, List<String>>> orderedTierFragments) {
        this.defaultKeyword = defaultKeyword;
        this.familyFragmentsByKeyword = familyFragmentsByKeyword;
        this.orderedTierFragments = orderedTierFragments;
    }

    @Nonnull
    public static ToolingConfig load(@Nonnull Path externalConfigRoot) {
        JsonObject root = ConfigResourceLoader.loadJsonObject(RESOURCE_PATH, externalConfigRoot);
        JsonObject tooling = ConfigResourceLoader.objectValue(root, "tooling");

        String defaultKeyword = ConfigResourceLoader.stringValue(tooling, "keywordDefault", "Tool_Hatchet_");

        Map<String, List<String>> families = parseFamilies(ConfigResourceLoader.objectValue(tooling, "families"));
        if (families.isEmpty()) {
            families = Map.of("tool_hatchet", List.of("tool_hatchet"));
        }

        EnumMap<ToolTier, List<String>> tierTokensByTier = parseTierTokens(ConfigResourceLoader.objectValue(tooling, "tiers"));
        if (tierTokensByTier.isEmpty()) {
            tierTokensByTier = defaultTierTokens();
        }

        List<Map.Entry<ToolTier, List<String>>> orderedFragments = new ArrayList<>();
        for (ToolTier tier : ToolTier.values()) {
            if (tier == ToolTier.NONE) {
                continue;
            }

            List<String> fragments = tierTokensByTier.getOrDefault(tier, List.of());
            if (!fragments.isEmpty()) {
                orderedFragments.add(Map.entry(tier, fragments));
            }
        }
        orderedFragments.sort((left, right) -> Integer.compare(right.getKey().rank(), left.getKey().rank()));

        return new ToolingConfig(defaultKeyword, Map.copyOf(families), List.copyOf(orderedFragments));
    }

    @Nonnull
    public String defaultKeyword() {
        return this.defaultKeyword;
    }

    public boolean matchesToolFamily(@Nonnull String normalizedItemId, @Nonnull String normalizedKeyword) {
        if (normalizedItemId.isBlank() || normalizedKeyword.isBlank()) {
            return false;
        }

        if (normalizedItemId.contains(normalizedKeyword)) {
            return true;
        }

        List<String> mapped = this.familyFragmentsByKeyword.get(normalizedKeyword);
        if (mapped == null) {
            return false;
        }

        for (String fragment : mapped) {
            if (normalizedItemId.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public ToolTier detectTier(@Nonnull String normalizedItemId) {
        for (Map.Entry<ToolTier, List<String>> entry : this.orderedTierFragments) {
            for (String fragment : entry.getValue()) {
                if (normalizedItemId.contains(fragment)) {
                    return entry.getKey();
                }
            }
        }
        return ToolTier.NONE;
    }

    @Nonnull
    private static Map<String, List<String>> parseFamilies(@Nonnull JsonObject familiesObject) {
        Map<String, List<String>> families = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : familiesObject.entrySet()) {
            String normalizedKeyword = normalizeToken(entry.getKey());
            List<String> fragments = parseTokens(entry.getValue());
            if (!normalizedKeyword.isEmpty() && !fragments.isEmpty()) {
                families.put(normalizedKeyword, fragments);
            }
        }
        return families;
    }

    @Nonnull
    private static EnumMap<ToolTier, List<String>> parseTierTokens(@Nonnull JsonObject tiersObject) {
        EnumMap<ToolTier, List<String>> tokensByTier = new EnumMap<>(ToolTier.class);
        for (Map.Entry<String, JsonElement> entry : tiersObject.entrySet()) {
            String rawTier = entry.getKey().trim();
            ToolTier tier = ToolTier.fromString(rawTier);
            if (tier == ToolTier.NONE && !"NONE".equalsIgnoreCase(rawTier)) {
                continue;
            }

            List<String> tokens = parseTokens(entry.getValue());
            if (!tokens.isEmpty()) {
                tokensByTier.put(tier, tokens);
            }
        }
        return tokensByTier;
    }

    @Nonnull
    private static EnumMap<ToolTier, List<String>> defaultTierTokens() {
        EnumMap<ToolTier, List<String>> defaults = new EnumMap<>(ToolTier.class);
        defaults.put(ToolTier.BRONZE, List.of("bronze"));
        defaults.put(ToolTier.IRON, List.of("iron"));
        defaults.put(ToolTier.STEEL, List.of("steel"));
        defaults.put(ToolTier.BLACK, List.of("black"));
        defaults.put(ToolTier.MITHRIL, List.of("mithril"));
        defaults.put(ToolTier.ADAMANT, List.of("adamant"));
        defaults.put(ToolTier.RUNE, List.of("rune"));
        defaults.put(ToolTier.DRAGON, List.of("dragon"));
        defaults.put(ToolTier.CRYSTAL, List.of("crystal"));
        return defaults;
    }

    @Nonnull
    private static List<String> parseTokens(@Nonnull JsonElement element) {
        List<String> parsed = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement tokenElement : element.getAsJsonArray()) {
                if (tokenElement == null || tokenElement.isJsonNull()) {
                    continue;
                }
                String normalized = normalizeToken(tokenElement.getAsString());
                if (!normalized.isEmpty()) {
                    parsed.add(normalized);
                }
            }
        } else if (element.isJsonPrimitive()) {
            for (String token : element.getAsString().split(",")) {
                String normalized = normalizeToken(token);
                if (!normalized.isEmpty()) {
                    parsed.add(normalized);
                }
            }
        }

        return parsed.isEmpty() ? List.of() : List.copyOf(parsed);
    }

    @Nonnull
    private static String normalizeToken(@Nonnull String raw) {
        String lowered = raw.trim().toLowerCase(Locale.ROOT);
        return lowered.replace('-', '_').replace(' ', '_');
    }
}
