package org.runetale.skills.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        int consumeQuantity = Math.max(1, ConfigResourceLoader.intValue(actionObject, "consumeQuantity", 1));
        String source = ConfigResourceLoader.stringValue(actionObject, "source", "item-action:" + id.toLowerCase(Locale.ROOT));
        boolean notifyPlayer = ConfigResourceLoader.booleanValue(actionObject, "notifyPlayer", true);
        boolean cancelInputEvent = ConfigResourceLoader.booleanValue(actionObject, "cancelInputEvent", true);
        boolean allowCreative = ConfigResourceLoader.booleanValue(actionObject, "allowCreative", false);

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
                buttonState);
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
            @Nonnull MouseButtonState mouseButtonState) {

        public boolean matchesItemId(@Nullable String heldItemId) {
            if (heldItemId == null) {
                return false;
            }

            String configured = this.itemId.trim();
            String held = heldItemId.trim();
            if (configured.equalsIgnoreCase(held)) {
                return true;
            }

            int heldNamespaceSeparator = held.lastIndexOf(':');
            if (heldNamespaceSeparator >= 0 && heldNamespaceSeparator + 1 < held.length()) {
                String heldWithoutNamespace = held.substring(heldNamespaceSeparator + 1);
                if (configured.equalsIgnoreCase(heldWithoutNamespace)) {
                    return true;
                }
            }

            int configuredNamespaceSeparator = configured.lastIndexOf(':');
            if (configuredNamespaceSeparator >= 0 && configuredNamespaceSeparator + 1 < configured.length()) {
                String configuredWithoutNamespace = configured.substring(configuredNamespaceSeparator + 1);
                return configuredWithoutNamespace.equalsIgnoreCase(held);
            }

            return false;
        }
    }
}
