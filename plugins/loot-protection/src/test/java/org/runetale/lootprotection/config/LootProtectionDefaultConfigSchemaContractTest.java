package org.runetale.lootprotection.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class LootProtectionDefaultConfigSchemaContractTest {

    private static final String RESOURCE_PATH = "LootProtection/config/loot-protection.json";
    private static final Set<String> REQUIRED_PATHS = Set.of(
            "enabled",
            "protectBlockBreakDrops",
            "protectKillDrops",
            "blockOwnership.enabled",
            "blockOwnership.inactivityResetMillis",
            "blockOwnership.notifyCooldownMillis",
            "dropClaim.windowMillis",
            "dropClaim.matchRadius",
            "ownerLock.enabled",
            "ownerLock.timeoutMillis",
            "ownerLock.retryIntervalMillis",
            "ownerLock.inventoryFullNotifyCooldownMillis");

    @Test
    void defaultConfigHasRequiredPaths() throws IOException {
        JsonObject root = loadJsonObject(RESOURCE_PATH);

        for (String path : REQUIRED_PATHS) {
            JsonElement value = valueAt(root, path);
            assertThat(value)
                    .as("required path %s in %s", path, RESOURCE_PATH)
                    .isNotNull();
            assertThat(value.isJsonNull())
                    .as("required path %s is non-null in %s", path, RESOURCE_PATH)
                    .isFalse();
        }
    }

    private static JsonObject loadJsonObject(String resourcePath) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(resourcePath)) {
            assertThat(input)
                    .as("resource exists: %s", resourcePath)
                    .isNotNull();
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                assertThat(parsed != null && parsed.isJsonObject())
                        .as("resource root is object: %s", resourcePath)
                        .isTrue();
                return parsed.getAsJsonObject();
            }
        }
    }

    private static JsonElement valueAt(JsonObject root, String dottedPath) {
        JsonObject currentObject = root;
        String[] segments = dottedPath.split("\\.");
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            JsonElement element = currentObject.get(segment);
            if (element == null) {
                return null;
            }
            if (i == segments.length - 1) {
                return element;
            }
            if (!element.isJsonObject()) {
                return null;
            }
            currentObject = element.getAsJsonObject();
        }
        return null;
    }
}
