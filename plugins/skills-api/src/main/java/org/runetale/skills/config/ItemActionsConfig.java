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
        @Nonnull String debugPluginKey) {

    private static final String RESOURCE_PATH = "Skills/Config/item-actions.json";

    @Nonnull
    public static ItemActionsConfig load(@Nonnull Path externalConfigRoot) {
        JsonObject root = ConfigResourceLoader.loadJsonObject(RESOURCE_PATH, externalConfigRoot);
        JsonObject debugConfig = ConfigResourceLoader.objectValue(root, "debug");

        return new ItemActionsConfig(
                parseActions(root.get("actions")),
                ConfigResourceLoader.stringValue(debugConfig, "pluginKey", "skills-actions"));
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
                requireTargetBlockMatchForReplacement);
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
            boolean requireTargetBlockMatchForReplacement) {

        public ItemXpActionDefinition {
            targetBlockIds = targetBlockIds == null || targetBlockIds.isEmpty() ? List.of() : List.copyOf(targetBlockIds);
            replaceTargetBlockId = replaceTargetBlockId == null ? "" : replaceTargetBlockId.trim();
            replaceTargetBlockDelayMillis = Math.max(0L, replaceTargetBlockDelayMillis);
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
                    true);
        }

        public boolean requiresItemConsumption() {
            return this.consumeQuantity > 0;
        }

        public boolean hasTargetBlockReplacement() {
            return !this.replaceTargetBlockId.isBlank();
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
