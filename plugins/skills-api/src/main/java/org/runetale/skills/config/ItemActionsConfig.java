package org.runetale.skills.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record ItemActionsConfig(
        @Nonnull List<ItemXpActionDefinition> actions,
        @Nonnull List<PlacementDespawnRule> placementDespawnRules,
        @Nonnull String debugPluginKey) {

    private static final String RESOURCE_PATH = "Skills/Config/item-actions.json";

    @Nonnull
    public static ItemActionsConfig load(@Nonnull Path externalConfigRoot) {
        JsonObject root = ConfigResourceLoader.loadJsonObject(RESOURCE_PATH, externalConfigRoot);
        JsonObject debugConfig = ConfigResourceLoader.objectValue(root, "debug");

        return new ItemActionsConfig(
                parseActions(root.get("actions")),
                parsePlacementDespawnRules(root.get("placementDespawnRules")),
                ConfigResourceLoader.stringValue(debugConfig, "pluginKey", "skills-actions"));
    }

    public ItemActionsConfig {
        actions = actions == null ? List.of() : List.copyOf(actions);
        placementDespawnRules = placementDespawnRules == null ? List.of() : List.copyOf(placementDespawnRules);
    }

    public ItemActionsConfig(
            @Nonnull List<ItemXpActionDefinition> actions,
            @Nonnull String debugPluginKey) {
        this(actions, List.of(), debugPluginKey);
    }

    @Nonnull
    private static List<ItemXpActionDefinition> parseActions(@Nullable JsonElement element) {
        List<ItemXpActionDefinition> parsed = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return List.of();
        }

        if (element.isJsonArray()) {
            int index = 0;
            for (JsonElement entry : element.getAsJsonArray()) {
                if (entry == null || !entry.isJsonObject()) {
                    index++;
                    continue;
                }

                ItemXpActionDefinition action = parseAction(entry.getAsJsonObject(), index++);
                if (action != null) {
                    parsed.add(action);
                }
            }
            return List.copyOf(parsed);
        }

        if (element.isJsonObject()) {
            ItemXpActionDefinition action = parseAction(element.getAsJsonObject(), 0);
            if (action != null) {
                parsed.add(action);
            }
        }
        return List.copyOf(parsed);
    }

    @Nullable
    private static ItemXpActionDefinition parseAction(@Nonnull JsonObject actionObject, int index) {
        String id = ConfigResourceLoader.stringValue(actionObject, "id", "item_action_" + index);
        boolean enabled = ConfigResourceLoader.booleanValue(actionObject, "enabled", true);
        String itemId = ConfigResourceLoader.stringValue(actionObject, "itemId", "");
        SkillType skillType = SkillType.tryParseStrict(ConfigResourceLoader.stringValue(actionObject, "skill", ""));
        double experience = ConfigResourceLoader.doubleValue(actionObject, "xp", 0.0D);
        int consumeQuantity = Math.max(0, ConfigResourceLoader.intValue(actionObject, "consumeQuantity", 1));
        String source = ConfigResourceLoader.stringValue(actionObject, "source", "item-action:" + id.toLowerCase(Locale.ROOT));
        boolean notifyPlayer = ConfigResourceLoader.booleanValue(actionObject, "notifyPlayer", true);
        boolean cancelInputEvent = ConfigResourceLoader.booleanValue(actionObject, "cancelInputEvent", true);
        boolean allowCreative = ConfigResourceLoader.booleanValue(actionObject, "allowCreative", false);
        List<String> targetBlockIds = parseTargetBlockIds(actionObject);
        String replaceTargetBlockId = ConfigResourceLoader.stringValue(actionObject, "replaceTargetBlockId", "").trim();
        long replaceTargetBlockDelayMillis = Math.max(0L, ConfigResourceLoader.longValue(actionObject, "replaceTargetBlockDelayMillis", 0L));
        boolean requireTargetBlockMatchForReplacement = ConfigResourceLoader.booleanValue(actionObject, "requireTargetBlockMatchForReplacement", true);
        long despawnDelayMillis = Math.max(0L, ConfigResourceLoader.longValue(actionObject, "despawnDelayMillis", 0L));
        BlockApplyMode despawnApplyMode = parseBlockApplyMode(
                ConfigResourceLoader.stringValue(actionObject, "despawnApplyMode", BlockApplyMode.SET_BLOCK.name()),
                BlockApplyMode.SET_BLOCK);
        String despawnSetBlockId = ConfigResourceLoader.stringValue(actionObject, "despawnSetBlockId", "Empty").trim();
        boolean requireTargetBlockMatchForDespawn = ConfigResourceLoader.booleanValue(actionObject, "requireTargetBlockMatchForDespawn", true);

        if (itemId.isBlank() || skillType == null || experience <= 0.0D) {
            return null;
        }

        JsonObject trigger = ConfigResourceLoader.objectValue(actionObject, "trigger");
        MouseButtonType buttonType = parseMouseButtonType(
                ConfigResourceLoader.stringValue(trigger, "mouseButton", MouseButtonType.Right.name()),
                MouseButtonType.Right);
        MouseButtonState buttonState = parseMouseButtonState(
                ConfigResourceLoader.stringValue(trigger, "mouseState", MouseButtonState.Pressed.name()),
                MouseButtonState.Pressed);

        return new ItemXpActionDefinition(
                id,
                enabled,
                itemId,
                skillType,
                experience,
                consumeQuantity,
                source,
                notifyPlayer,
                cancelInputEvent,
                allowCreative,
                buttonType,
                buttonState,
                targetBlockIds,
                replaceTargetBlockId,
                replaceTargetBlockDelayMillis,
                requireTargetBlockMatchForReplacement,
                despawnDelayMillis,
                despawnApplyMode,
                despawnSetBlockId,
                requireTargetBlockMatchForDespawn);
    }

    @Nonnull
    private static List<PlacementDespawnRule> parsePlacementDespawnRules(@Nullable JsonElement element) {
        List<PlacementDespawnRule> parsed = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return List.of();
        }

        if (element.isJsonArray()) {
            int index = 0;
            for (JsonElement entry : element.getAsJsonArray()) {
                if (entry == null || !entry.isJsonObject()) {
                    index++;
                    continue;
                }

                PlacementDespawnRule rule = parsePlacementDespawnRule(entry.getAsJsonObject(), index++);
                if (rule != null) {
                    parsed.add(rule);
                }
            }
            return List.copyOf(parsed);
        }

        if (element.isJsonObject()) {
            PlacementDespawnRule rule = parsePlacementDespawnRule(element.getAsJsonObject(), 0);
            if (rule != null) {
                parsed.add(rule);
            }
        }
        return List.copyOf(parsed);
    }

    @Nullable
    private static PlacementDespawnRule parsePlacementDespawnRule(@Nonnull JsonObject ruleObject, int index) {
        String id = ConfigResourceLoader.stringValue(ruleObject, "id", "placement_despawn_" + index);
        boolean enabled = ConfigResourceLoader.booleanValue(ruleObject, "enabled", true);
        List<String> targetBlockIds = parseTargetBlockIds(ruleObject);
        long delayMillis = Math.max(0L, ConfigResourceLoader.longValue(ruleObject, "delayMillis", 0L));
        BlockApplyMode applyMode = parseBlockApplyMode(
                ConfigResourceLoader.stringValue(ruleObject, "applyMode", BlockApplyMode.SET_BLOCK.name()),
                BlockApplyMode.SET_BLOCK);
        String setBlockId = ConfigResourceLoader.stringValue(ruleObject, "setBlockId", "Empty").trim();
        boolean requireTargetBlockMatch = ConfigResourceLoader.booleanValue(ruleObject, "requireTargetBlockMatch", true);

        if (targetBlockIds.isEmpty() || delayMillis <= 0L) {
            return null;
        }
        if (applyMode == BlockApplyMode.SET_BLOCK && setBlockId.isBlank()) {
            return null;
        }

        return new PlacementDespawnRule(
                id,
                enabled,
                targetBlockIds,
                delayMillis,
                applyMode,
                setBlockId,
                requireTargetBlockMatch);
    }

    @Nonnull
    private static List<String> parseTargetBlockIds(@Nonnull JsonObject actionObject) {
        Set<String> targetBlockIds = new LinkedHashSet<>();

        JsonElement listElement = actionObject.get("targetBlockIds");
        if (listElement != null && listElement.isJsonArray()) {
            for (JsonElement entry : listElement.getAsJsonArray()) {
                if (entry == null || entry.isJsonNull()) {
                    continue;
                }

                String raw;
                try {
                    raw = entry.getAsString();
                } catch (RuntimeException ignored) {
                    continue;
                }

                String normalized = raw.trim();
                if (!normalized.isBlank()) {
                    targetBlockIds.add(normalized);
                }
            }
        }

        String singleBlockId = ConfigResourceLoader.stringValue(actionObject, "targetBlockId", "");
        if (!singleBlockId.isBlank()) {
            targetBlockIds.add(singleBlockId);
        }

        if (targetBlockIds.isEmpty()) {
            return List.of();
        }

        return List.copyOf(targetBlockIds);
    }

    @Nonnull
    private static MouseButtonType parseMouseButtonType(@Nullable String raw, @Nonnull MouseButtonType defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        String normalized = raw.trim();
        for (MouseButtonType candidate : MouseButtonType.values()) {
            if (candidate.name().equalsIgnoreCase(normalized)) {
                return candidate;
            }
        }
        return defaultValue;
    }

    @Nonnull
    private static MouseButtonState parseMouseButtonState(@Nullable String raw, @Nonnull MouseButtonState defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        String normalized = raw.trim();
        for (MouseButtonState candidate : MouseButtonState.values()) {
            if (candidate.name().equalsIgnoreCase(normalized)) {
                return candidate;
            }
        }
        return defaultValue;
    }

    @Nonnull
    private static BlockApplyMode parseBlockApplyMode(@Nullable String raw, @Nonnull BlockApplyMode defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        String normalized = raw.trim();
        for (BlockApplyMode candidate : BlockApplyMode.values()) {
            if (candidate.name().equalsIgnoreCase(normalized)) {
                return candidate;
            }
        }
        return defaultValue;
    }

    public enum BlockApplyMode {
        SET_BLOCK,
        NATURAL_REMOVE
    }

    public record PlacementDespawnRule(
            @Nonnull String id,
            boolean enabled,
            @Nonnull List<String> targetBlockIds,
            long delayMillis,
            @Nonnull BlockApplyMode applyMode,
            @Nullable String setBlockId,
            boolean requireTargetBlockMatch) {

        public PlacementDespawnRule {
            targetBlockIds = targetBlockIds == null || targetBlockIds.isEmpty() ? List.of() : List.copyOf(targetBlockIds);
            delayMillis = Math.max(0L, delayMillis);
            applyMode = applyMode == null ? BlockApplyMode.SET_BLOCK : applyMode;
            setBlockId = setBlockId == null ? "" : setBlockId.trim();
        }

        public boolean hasDespawnAction() {
            if (!this.enabled || this.delayMillis <= 0L) {
                return false;
            }
            return this.applyMode == BlockApplyMode.NATURAL_REMOVE || !this.setBlockId.isBlank();
        }

        public boolean matchesTargetBlockId(@Nullable String targetBlockId) {
            if (this.targetBlockIds.isEmpty() || targetBlockId == null) {
                return false;
            }

            for (String configuredBlockId : this.targetBlockIds) {
                if (ItemXpActionDefinition.idsMatch(configuredBlockId, targetBlockId)) {
                    return true;
                }
            }

            return false;
        }
    }

    public record ItemXpActionDefinition(
            @Nonnull String id,
            boolean enabled,
            @Nonnull String itemId,
            @Nonnull SkillType skillType,
            double experience,
            int consumeQuantity,
            @Nonnull String source,
            boolean notifyPlayer,
            boolean cancelInputEvent,
            boolean allowCreative,
            @Nonnull MouseButtonType mouseButtonType,
            @Nonnull MouseButtonState mouseButtonState,
            @Nonnull List<String> targetBlockIds,
            @Nullable String replaceTargetBlockId,
            long replaceTargetBlockDelayMillis,
            boolean requireTargetBlockMatchForReplacement,
            long despawnDelayMillis,
            @Nonnull BlockApplyMode despawnApplyMode,
            @Nullable String despawnSetBlockId,
            boolean requireTargetBlockMatchForDespawn) {

        public ItemXpActionDefinition {
            targetBlockIds = targetBlockIds == null || targetBlockIds.isEmpty() ? List.of() : List.copyOf(targetBlockIds);
            replaceTargetBlockId = replaceTargetBlockId == null ? "" : replaceTargetBlockId.trim();
            replaceTargetBlockDelayMillis = Math.max(0L, replaceTargetBlockDelayMillis);
            despawnDelayMillis = Math.max(0L, despawnDelayMillis);
            despawnApplyMode = despawnApplyMode == null ? BlockApplyMode.SET_BLOCK : despawnApplyMode;
            despawnSetBlockId = despawnSetBlockId == null ? "" : despawnSetBlockId.trim();
        }

        public ItemXpActionDefinition(
                @Nonnull String id,
                boolean enabled,
                @Nonnull String itemId,
                @Nonnull SkillType skillType,
                double experience,
                int consumeQuantity,
                @Nonnull String source,
                boolean notifyPlayer,
                boolean cancelInputEvent,
                boolean allowCreative,
                @Nonnull MouseButtonType mouseButtonType,
                @Nonnull MouseButtonState mouseButtonState,
                @Nonnull List<String> targetBlockIds) {
            this(
                    id,
                    enabled,
                    itemId,
                    skillType,
                    experience,
                    consumeQuantity,
                    source,
                    notifyPlayer,
                    cancelInputEvent,
                    allowCreative,
                    mouseButtonType,
                    mouseButtonState,
                    targetBlockIds,
                    "",
                    0L,
                    true,
                    0L,
                    BlockApplyMode.SET_BLOCK,
                    "",
                    true);
        }

        public ItemXpActionDefinition(
                @Nonnull String id,
                boolean enabled,
                @Nonnull String itemId,
                @Nonnull SkillType skillType,
                double experience,
                int consumeQuantity,
                @Nonnull String source,
                boolean notifyPlayer,
                boolean cancelInputEvent,
                boolean allowCreative,
                @Nonnull MouseButtonType mouseButtonType,
                @Nonnull MouseButtonState mouseButtonState) {
            this(
                    id,
                    enabled,
                    itemId,
                    skillType,
                    experience,
                    consumeQuantity,
                    source,
                    notifyPlayer,
                    cancelInputEvent,
                    allowCreative,
                    mouseButtonType,
                    mouseButtonState,
                    List.of(),
                    "",
                    0L,
                    true,
                    0L,
                    BlockApplyMode.SET_BLOCK,
                    "",
                    true);
        }

        public boolean requiresItemConsumption() {
            return this.consumeQuantity > 0;
        }

        public boolean hasTargetBlockReplacement() {
            return !this.replaceTargetBlockId.isBlank();
        }

        public boolean hasDespawnAction() {
            if (this.despawnDelayMillis <= 0L) {
                return false;
            }
            return this.despawnApplyMode == BlockApplyMode.NATURAL_REMOVE || !this.despawnSetBlockId.isBlank();
        }

        public boolean matchesInteractionType(@Nonnull InteractionType interactionType) {
            if (this.mouseButtonState != MouseButtonState.Pressed) {
                return false;
            }

            return switch (interactionType) {
                case Primary -> this.mouseButtonType == MouseButtonType.Left;
                case Secondary, Use -> this.mouseButtonType == MouseButtonType.Right;
                default -> false;
            };
        }

        public boolean matchesItemId(@Nullable String heldItemId) {
            if (heldItemId == null) {
                return false;
            }

            return idsMatch(this.itemId, heldItemId);
        }

        public boolean matchesTargetBlockId(@Nullable String targetBlockId) {
            if (this.targetBlockIds.isEmpty()) {
                return true;
            }
            if (targetBlockId == null) {
                return false;
            }

            for (String configuredBlockId : this.targetBlockIds) {
                if (idsMatch(configuredBlockId, targetBlockId)) {
                    return true;
                }
            }

            return false;
        }

        public static boolean idsMatch(@Nonnull String configuredId, @Nonnull String actualId) {
            String configured = configuredId.trim();
            String actual = actualId.trim();
            if (configured.equalsIgnoreCase(actual)) {
                return true;
            }

            int actualNamespaceSeparator = actual.lastIndexOf(':');
            if (actualNamespaceSeparator >= 0 && actualNamespaceSeparator + 1 < actual.length()) {
                String actualWithoutNamespace = actual.substring(actualNamespaceSeparator + 1);
                if (configured.equalsIgnoreCase(actualWithoutNamespace)) {
                    return true;
                }
            }

            int configuredNamespaceSeparator = configured.lastIndexOf(':');
            if (configuredNamespaceSeparator >= 0 && configuredNamespaceSeparator + 1 < configured.length()) {
                String configuredWithoutNamespace = configured.substring(configuredNamespaceSeparator + 1);
                return configuredWithoutNamespace.equalsIgnoreCase(actual);
            }

            return false;
        }
    }
}
