package org.runetale.skills.config;

import org.runetale.skills.domain.ToolTier;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class ToolingConfig {

    private static final String RESOURCE_PATH = "Skills/Config/tooling.properties";
    private static final String LEGACY_DEFAULTS_RESOURCE_PATH = "Skills/tool-tier-defaults.properties";
    private static final String FAMILY_PREFIX = "family.";
    private static final String TIER_PREFIX = "tier.";

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
    public static ToolingConfig load() {
        Properties properties = ConfigResourceLoader.loadProperties(RESOURCE_PATH);
        Properties legacyDefaults = ConfigResourceLoader.loadProperties(LEGACY_DEFAULTS_RESOURCE_PATH);

        String defaultKeyword = ConfigResourceLoader.stringValue(
                properties,
                "keyword.default",
                ConfigResourceLoader.stringValue(legacyDefaults, "keyword.default", "Tool_Hatchet_"));

        Map<String, List<String>> families = parseFamilies(properties);
        if (families.isEmpty()) {
            families = parseLegacyFamilies(legacyDefaults);
        }
        if (families.isEmpty()) {
            families = Map.of("tool_hatchet", List.of("tool_hatchet"));
        }

        EnumMap<ToolTier, List<String>> tierTokensByTier = parseTierTokens(properties);
        if (tierTokensByTier.isEmpty()) {
            tierTokensByTier = parseLegacyTierTokens(legacyDefaults);
        }
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
    private static Map<String, List<String>> parseFamilies(@Nonnull Properties properties) {
        Map<String, List<String>> families = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(FAMILY_PREFIX)) {
                continue;
            }

            String normalizedKeyword = normalizeToken(key.substring(FAMILY_PREFIX.length()));
            List<String> fragments = parseCsvTokens(properties.getProperty(key));
            if (!normalizedKeyword.isEmpty() && !fragments.isEmpty()) {
                families.put(normalizedKeyword, fragments);
            }
        }
        return families;
    }

    @Nonnull
    private static Map<String, List<String>> parseLegacyFamilies(@Nonnull Properties legacyDefaults) {
        Map<String, List<String>> families = new HashMap<>();
        for (String key : legacyDefaults.stringPropertyNames()) {
            if (!key.startsWith("keyword.") || !key.endsWith(".tiers")) {
                continue;
            }

            String normalizedKeyword = normalizeToken(key.substring("keyword.".length(), key.length() - ".tiers".length()));
            List<String> fragments = parseCsvTokens(legacyDefaults.getProperty(key));
            if (!normalizedKeyword.isEmpty() && !fragments.isEmpty()) {
                families.put(normalizedKeyword, fragments);
            }
        }
        return families;
    }

    @Nonnull
    private static EnumMap<ToolTier, List<String>> parseTierTokens(@Nonnull Properties properties) {
        EnumMap<ToolTier, List<String>> tokensByTier = new EnumMap<>(ToolTier.class);
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(TIER_PREFIX)) {
                continue;
            }

            String rawTier = key.substring(TIER_PREFIX.length()).trim();
            ToolTier tier = ToolTier.fromString(rawTier);
            if (tier == ToolTier.NONE && !"NONE".equalsIgnoreCase(rawTier)) {
                continue;
            }

            List<String> tokens = parseCsvTokens(properties.getProperty(key));
            if (!tokens.isEmpty()) {
                tokensByTier.put(tier, tokens);
            }
        }
        return tokensByTier;
    }

    @Nonnull
    private static EnumMap<ToolTier, List<String>> parseLegacyTierTokens(@Nonnull Properties legacyDefaults) {
        EnumMap<ToolTier, List<String>> tokensByTier = new EnumMap<>(ToolTier.class);
        for (ToolTier tier : ToolTier.values()) {
            String alias = legacyDefaults.getProperty("alias." + tier.name().toLowerCase(Locale.ROOT));
            if (alias != null && !alias.isBlank()) {
                tokensByTier.put(tier, List.of(normalizeToken(alias)));
            }
        }
        return tokensByTier;
    }

    @Nonnull
    private static EnumMap<ToolTier, List<String>> defaultTierTokens() {
        EnumMap<ToolTier, List<String>> defaults = new EnumMap<>(ToolTier.class);
        defaults.put(ToolTier.WOOD, List.of("wood", "bronze"));
        defaults.put(ToolTier.CRUDE, List.of("crude", "steel"));
        defaults.put(ToolTier.COPPER, List.of("copper"));
        defaults.put(ToolTier.IRON, List.of("iron"));
        defaults.put(ToolTier.THORIUM, List.of("thorium"));
        defaults.put(ToolTier.COBALT, List.of("cobalt"));
        defaults.put(ToolTier.ADAMANTITE, List.of("adamant", "adamantite"));
        defaults.put(ToolTier.ONYXIUM, List.of("onyxium", "rune"));
        defaults.put(ToolTier.MITHRIL, List.of("mithril", "dragon", "crystal"));
        return defaults;
    }

    @Nonnull
    private static List<String> parseCsvTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        List<String> parsed = new ArrayList<>();
        for (String token : raw.split(",")) {
            String normalized = normalizeToken(token);
            if (!normalized.isEmpty()) {
                parsed.add(normalized);
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
