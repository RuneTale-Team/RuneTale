# StarterKit Plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone starterkit plugin that grants configurable starter gear to first-time players using ECS-driven detection.

**Architecture:** A `GrantStarterKitSystem` (HolderSystem) queries for player entities missing a `ReceivedStarterKitComponent`. On entity add, it reads a JSON kit config, places items into the player's inventory containers, and attaches the marker component. Config follows the PathLayout + ExternalConfigBootstrap + ConfigService pattern used by block-regeneration and loot-protection.

**Tech Stack:** Java 25, Hytale Server API, GSON (JsonParser), JUnit 5, AssertJ, Mockito

**Spec:** `docs/superpowers/specs/2026-04-01-starterkit-plugin-design.md`

---

## File Structure

```
plugins/starterkit/
  src/main/java/org/runetale/starterkit/
    StarterKitPlugin.java                       # JavaPlugin entry point
    config/
      StarterKitPathLayout.java                 # Path resolution
      StarterKitExternalConfigBootstrap.java    # Seeds default config
      StarterKitConfigService.java              # JSON parser -> StarterKitConfig
    domain/
      StarterKitConfig.java                     # Record: version, enabled, items
      KitItem.java                              # Record: container, itemId, quantity
    component/
      ReceivedStarterKitComponent.java          # ECS marker component
    system/
      GrantStarterKitSystem.java                # HolderSystem granting items
  src/main/resources/
    manifest.json
    StarterKit/
      config/
        kit.json                                # Default kit definition
  src/test/java/org/runetale/starterkit/
    config/
      StarterKitPathLayoutTest.java
      StarterKitExternalConfigBootstrapContractTest.java
      StarterKitConfigServiceTest.java
    domain/
      KitItemTest.java
    manifest/
      StarterKitManifestContractTest.java
```

Build files to modify:
- `settings.gradle.kts` — add `:plugins:starterkit`
- `build.gradle.kts` — add starterkit config deploy tasks + wire into `deployPluginsToRun`

---

### Task 1: Gradle Module Scaffold

**Files:**
- Modify: `settings.gradle.kts:24` (add include)
- Create: `plugins/starterkit/src/main/resources/manifest.json`
- Create: `plugins/starterkit/src/main/resources/StarterKit/config/kit.json`

- [ ] **Step 1: Add module to settings.gradle.kts**

Add after the `loot-protection` include at line 24:

```
include(":plugins:starterkit")
```

- [ ] **Step 2: Create manifest.json**

Create `plugins/starterkit/src/main/resources/manifest.json`:

```json
{
  "Group": "RuneTale",
  "Name": "StarterKitPlugin",
  "Version": "0.1.0",
  "Main": "org.runetale.starterkit.StarterKitPlugin",
  "Authors": [
    {
      "Name": "Nico Piel",
      "Email": "nicopiel@mailbox.org"
    }
  ],
  "ServerVersion": "${hytaleServerVersion}",
  "IncludesAssetPack": false
}
```

- [ ] **Step 3: Create default kit.json**

Create `plugins/starterkit/src/main/resources/StarterKit/config/kit.json`:

```json
{
  "version": 1,
  "enabled": true,
  "items": [
    { "container": "hotbar", "itemId": "Weapon_Sword_Steel_Rusty", "quantity": 1 },
    { "container": "hotbar", "itemId": "Food_Bread", "quantity": 9 },
    { "container": "armour", "itemId": "Armor_Leather_Light_Legs", "quantity": 1 },
    { "container": "utility", "itemId": "Weapon_Shield_Rusty", "quantity": 1 }
  ]
}
```

- [ ] **Step 4: Verify the module resolves**

Run: `./gradlew :plugins:starterkit:dependencies --configuration compileClasspath`
Expected: resolves `com.hypixel.hytale:Server` dependency without errors.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts plugins/starterkit/src/main/resources/
git commit -m "Scaffold starterkit plugin module with manifest and default config"
```

---

### Task 2: Domain Records (KitItem + StarterKitConfig)

**Files:**
- Create: `plugins/starterkit/src/main/java/org/runetale/starterkit/domain/KitItem.java`
- Create: `plugins/starterkit/src/main/java/org/runetale/starterkit/domain/StarterKitConfig.java`
- Create: `plugins/starterkit/src/test/java/org/runetale/starterkit/domain/KitItemTest.java`

- [ ] **Step 1: Write KitItem tests**

Create `plugins/starterkit/src/test/java/org/runetale/starterkit/domain/KitItemTest.java`:

```java
package org.runetale.starterkit.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KitItemTest {

    @Test
    void validKitItemIsCreated() {
        KitItem item = new KitItem("hotbar", "Weapon_Sword_Steel_Rusty", 1);

        assertThat(item.container()).isEqualTo("hotbar");
        assertThat(item.itemId()).isEqualTo("Weapon_Sword_Steel_Rusty");
        assertThat(item.quantity()).isEqualTo(1);
    }

    @Test
    void blankContainerThrows() {
        assertThatThrownBy(() -> new KitItem("", "Weapon_Sword_Steel_Rusty", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("container");
    }

    @Test
    void blankItemIdThrows() {
        assertThatThrownBy(() -> new KitItem("hotbar", " ", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itemId");
    }

    @Test
    void zeroQuantityThrows() {
        assertThatThrownBy(() -> new KitItem("hotbar", "Food_Bread", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");
    }

    @Test
    void negativeQuantityThrows() {
        assertThatThrownBy(() -> new KitItem("hotbar", "Food_Bread", -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");
    }

    @Test
    void unknownContainerThrows() {
        assertThatThrownBy(() -> new KitItem("banana", "Food_Bread", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("container");
    }

    @Test
    void allValidContainersAreAccepted() {
        for (String container : new String[]{"hotbar", "armour", "utility", "tools", "storage", "backpack"}) {
            KitItem item = new KitItem(container, "Food_Bread", 1);
            assertThat(item.container()).isEqualTo(container);
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :plugins:starterkit:test`
Expected: compilation failure — `KitItem` class does not exist.

- [ ] **Step 3: Implement KitItem**

Create `plugins/starterkit/src/main/java/org/runetale/starterkit/domain/KitItem.java`:

```java
package org.runetale.starterkit.domain;

import javax.annotation.Nonnull;
import java.util.Set;

public record KitItem(@Nonnull String container, @Nonnull String itemId, int quantity) {

    private static final Set<String> VALID_CONTAINERS = Set.of(
            "hotbar", "armour", "utility", "tools", "storage", "backpack");

    public KitItem {
        if (container == null || container.isBlank() || !VALID_CONTAINERS.contains(container)) {
            throw new IllegalArgumentException(
                    "container must be one of " + VALID_CONTAINERS + " but was: '" + container + "'");
        }
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive but was: " + quantity);
        }
    }
}
```

- [ ] **Step 4: Implement StarterKitConfig**

Create `plugins/starterkit/src/main/java/org/runetale/starterkit/domain/StarterKitConfig.java`:

```java
package org.runetale.starterkit.domain;

import javax.annotation.Nonnull;
import java.util.List;

public record StarterKitConfig(int version, boolean enabled, @Nonnull List<KitItem> items) {

    public static final int DEFAULT_VERSION = 1;

    public StarterKitConfig {
        if (items == null) {
            items = List.of();
        }
    }

    @Nonnull
    public static StarterKitConfig defaults() {
        return new StarterKitConfig(DEFAULT_VERSION, true, List.of());
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :plugins:starterkit:test`
Expected: all 7 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add plugins/starterkit/src/main/java/org/runetale/starterkit/domain/ \
       plugins/starterkit/src/test/java/org/runetale/starterkit/domain/
git commit -m "Add KitItem and StarterKitConfig domain records with validation"
```

---

### Task 3: Config Infrastructure (PathLayout + Bootstrap + ConfigService)

**Files:**
- Create: `plugins/starterkit/src/main/java/org/runetale/starterkit/config/StarterKitPathLayout.java`
- Create: `plugins/starterkit/src/main/java/org/runetale/starterkit/config/StarterKitExternalConfigBootstrap.java`
- Create: `plugins/starterkit/src/main/java/org/runetale/starterkit/config/StarterKitConfigService.java`
- Create: `plugins/starterkit/src/test/java/org/runetale/starterkit/config/StarterKitPathLayoutTest.java`
- Create: `plugins/starterkit/src/test/java/org/runetale/starterkit/config/StarterKitExternalConfigBootstrapContractTest.java`
- Create: `plugins/starterkit/src/test/java/org/runetale/starterkit/config/StarterKitConfigServiceTest.java`

- [ ] **Step 1: Write PathLayout tests**

Create `plugins/starterkit/src/test/java/org/runetale/starterkit/config/StarterKitPathLayoutTest.java`:

```java
package org.runetale.starterkit.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StarterKitPathLayoutTest {

    @Test
    void fromDataDirectoryBuildsExpectedRoots(@TempDir Path tempDir) {
        Path dataDirectory = tempDir.resolve("mods").resolve("starterkit-data");

        StarterKitPathLayout layout = StarterKitPathLayout.fromDataDirectory(dataDirectory);

        assertThat(layout.modsRoot()).isEqualTo(tempDir.resolve("mods"));
        assertThat(layout.pluginConfigRoot())
                .isEqualTo(tempDir.resolve("mods").resolve("runetale").resolve("config").resolve("starterkit"));
    }

    @Test
    void externalRelativeResourcePathStripsStarterKitPrefix() {
        assertThat(StarterKitPathLayout.externalRelativeResourcePath("StarterKit/config/kit.json"))
                .isEqualTo("config/kit.json");
        assertThat(StarterKitPathLayout.externalRelativeResourcePath("config/kit.json"))
                .isEqualTo("config/kit.json");
    }

    @Test
    void resolveConfigResourcePathMapsIntoPluginConfigRoot(@TempDir Path tempDir) {
        StarterKitPathLayout layout = StarterKitPathLayout.fromDataDirectory(
                tempDir.resolve("mods").resolve("starterkit-data"));

        Path resolved = layout.resolveConfigResourcePath("StarterKit/config/kit.json");

        assertThat(resolved).isEqualTo(layout.pluginConfigRoot().resolve("config").resolve("kit.json"));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :plugins:starterkit:test`
Expected: compilation failure — `StarterKitPathLayout` does not exist.

- [ ] **Step 3: Implement StarterKitPathLayout**

Create `plugins/starterkit/src/main/java/org/runetale/starterkit/config/StarterKitPathLayout.java`:

```java
package org.runetale.starterkit.config;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;

public final class StarterKitPathLayout {

    private static final String RUNETALE_NAMESPACE = "runetale";
    private static final String PLUGIN_ID = "starterkit";

    private final Path modsRoot;
    private final Path pluginConfigRoot;

    private StarterKitPathLayout(@Nonnull Path modsRoot, @Nonnull Path pluginConfigRoot) {
        this.modsRoot = modsRoot;
        this.pluginConfigRoot = pluginConfigRoot;
    }

    @Nonnull
    public static StarterKitPathLayout fromDataDirectory(@Nonnull Path dataDirectory) {
        Path modsRoot = dataDirectory.getParent();
        if (modsRoot == null) {
            modsRoot = dataDirectory;
        }

        Path configRoot = modsRoot.resolve(RUNETALE_NAMESPACE).resolve("config").resolve(PLUGIN_ID);
        return new StarterKitPathLayout(modsRoot, configRoot);
    }

    @Nonnull
    public Path modsRoot() {
        return this.modsRoot;
    }

    @Nonnull
    public Path pluginConfigRoot() {
        return this.pluginConfigRoot;
    }

    @Nonnull
    public Path resolveConfigResourcePath(@Nonnull String resourcePath) {
        String relative = externalRelativeResourcePath(resourcePath);
        return this.pluginConfigRoot.resolve(relative.replace('/', File.separatorChar));
    }

    @Nonnull
    public static String externalRelativeResourcePath(@Nonnull String resourcePath) {
        if (resourcePath.startsWith("StarterKit/")) {
            return resourcePath.substring("StarterKit/".length());
        }
        return resourcePath;
    }
}
```

- [ ] **Step 4: Run PathLayout tests to verify they pass**

Run: `./gradlew :plugins:starterkit:test --tests '*StarterKitPathLayoutTest'`
Expected: all 3 tests PASS.

- [ ] **Step 5: Write ExternalConfigBootstrap contract tests**

Create `plugins/starterkit/src/test/java/org/runetale/starterkit/config/StarterKitExternalConfigBootstrapContractTest.java`:

```java
package org.runetale.starterkit.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class StarterKitExternalConfigBootstrapContractTest {

    @Test
    void seedMissingDefaultsCopiesKitConfig(@TempDir Path tempDir) {
        StarterKitPathLayout layout = StarterKitPathLayout.fromDataDirectory(
                tempDir.resolve("mods").resolve("starterkit-data"));

        StarterKitExternalConfigBootstrap.seedMissingDefaults(layout);

        assertThat(layout.resolveConfigResourcePath("StarterKit/config/kit.json")).exists();
    }

    @Test
    void seedMissingDefaultsDoesNotOverwriteExistingFiles(@TempDir Path tempDir) throws IOException {
        StarterKitPathLayout layout = StarterKitPathLayout.fromDataDirectory(
                tempDir.resolve("mods").resolve("starterkit-data"));
        Path kitPath = layout.resolveConfigResourcePath("StarterKit/config/kit.json");
        Files.createDirectories(kitPath.getParent());
        Files.writeString(kitPath, "{\"version\":99}\n");

        StarterKitExternalConfigBootstrap.seedMissingDefaults(layout);

        assertThat(Files.readString(kitPath)).isEqualTo("{\"version\":99}\n");
    }
}
```

- [ ] **Step 6: Implement StarterKitExternalConfigBootstrap**

Create `plugins/starterkit/src/main/java/org/runetale/starterkit/config/StarterKitExternalConfigBootstrap.java`:

```java
package org.runetale.starterkit.config;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StarterKitExternalConfigBootstrap {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String KIT_CONFIG_RESOURCE = "StarterKit/config/kit.json";

    private StarterKitExternalConfigBootstrap() {
    }

    public static void seedMissingDefaults(@Nonnull StarterKitPathLayout pathLayout) {
        Path configRoot = pathLayout.pluginConfigRoot();
        LOGGER.atInfo().log("[StarterKit] Seeding external config defaults configRoot=%s", configRoot);

        try {
            Files.createDirectories(configRoot);
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log(
                    "[StarterKit] Failed to initialize config directory=%s", configRoot);
            return;
        }

        if (copyClasspathResourceIfMissing(pathLayout, KIT_CONFIG_RESOURCE)) {
            LOGGER.atInfo().log("[StarterKit] External config bootstrap complete copied=1 root=%s", configRoot);
            return;
        }
        LOGGER.atInfo().log("[StarterKit] External config bootstrap complete copied=0 root=%s", configRoot);
    }

    private static boolean copyClasspathResourceIfMissing(
            @Nonnull StarterKitPathLayout pathLayout,
            @Nonnull String resourcePath) {
        Path outputPath = pathLayout.resolveConfigResourcePath(resourcePath);
        if (Files.exists(outputPath)) {
            LOGGER.atFine().log("[StarterKit] Bootstrap skipped existing config file: %s", outputPath);
            return false;
        }

        try {
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log(
                    "[StarterKit] Failed to create config directory for %s", outputPath);
            return false;
        }

        try (InputStream input = openClasspathResource(resourcePath)) {
            if (input == null) {
                LOGGER.atWarning().log("[StarterKit] Missing bundled bootstrap resource=%s", resourcePath);
                return false;
            }

            Files.copy(input, outputPath);
            LOGGER.atInfo().log("[StarterKit] Seeded default config file: %s", outputPath);
            return true;
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log(
                    "[StarterKit] Failed writing bootstrap resource=%s path=%s", resourcePath, outputPath);
            return false;
        }
    }

    @Nullable
    private static InputStream openClasspathResource(@Nonnull String resourcePath) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader selectedClassLoader = StarterKitExternalConfigBootstrap.class.getClassLoader();
        if (selectedClassLoader == null) {
            selectedClassLoader = contextClassLoader;
        }
        if (selectedClassLoader == null) {
            return null;
        }

        InputStream selectedInput = selectedClassLoader.getResourceAsStream(resourcePath);
        if (selectedInput == null && contextClassLoader != null && contextClassLoader != selectedClassLoader) {
            selectedInput = contextClassLoader.getResourceAsStream(resourcePath);
        }
        return selectedInput;
    }
}
```

- [ ] **Step 7: Write ConfigService tests**

Create `plugins/starterkit/src/test/java/org/runetale/starterkit/config/StarterKitConfigServiceTest.java`:

```java
package org.runetale.starterkit.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.starterkit.domain.StarterKitConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StarterKitConfigServiceTest {

    @Test
    void loadParsesValidConfig(@TempDir Path tempDir) throws IOException {
        StarterKitPathLayout layout = StarterKitPathLayout.fromDataDirectory(
                tempDir.resolve("mods").resolve("starterkit-data"));
        Path kitPath = layout.resolveConfigResourcePath("StarterKit/config/kit.json");
        Files.createDirectories(kitPath.getParent());
        Files.writeString(kitPath, """
                {
                  "version": 1,
                  "enabled": true,
                  "items": [
                    { "container": "hotbar", "itemId": "Weapon_Sword_Steel_Rusty", "quantity": 1 },
                    { "container": "armour", "itemId": "Armor_Leather_Light_Legs", "quantity": 1 }
                  ]
                }
                """);

        StarterKitConfig config = new StarterKitConfigService(layout.pluginConfigRoot()).load();

        assertThat(config.version()).isEqualTo(1);
        assertThat(config.enabled()).isTrue();
        assertThat(config.items()).hasSize(2);
        assertThat(config.items().get(0).container()).isEqualTo("hotbar");
        assertThat(config.items().get(0).itemId()).isEqualTo("Weapon_Sword_Steel_Rusty");
        assertThat(config.items().get(0).quantity()).isEqualTo(1);
        assertThat(config.items().get(1).container()).isEqualTo("armour");
    }

    @Test
    void loadReturnsDefaultsWhenFileMissing(@TempDir Path tempDir) {
        StarterKitPathLayout layout = StarterKitPathLayout.fromDataDirectory(
                tempDir.resolve("mods").resolve("starterkit-data"));

        StarterKitConfig config = new StarterKitConfigService(layout.pluginConfigRoot()).load();

        assertThat(config.enabled()).isTrue();
        assertThat(config.items()).isEmpty();
    }

    @Test
    void loadReturnsDefaultsWhenJsonIsInvalid(@TempDir Path tempDir) throws IOException {
        StarterKitPathLayout layout = StarterKitPathLayout.fromDataDirectory(
                tempDir.resolve("mods").resolve("starterkit-data"));
        Path kitPath = layout.resolveConfigResourcePath("StarterKit/config/kit.json");
        Files.createDirectories(kitPath.getParent());
        Files.writeString(kitPath, "not valid json {{{");

        StarterKitConfig config = new StarterKitConfigService(layout.pluginConfigRoot()).load();

        assertThat(config.enabled()).isTrue();
        assertThat(config.items()).isEmpty();
    }

    @Test
    void loadSkipsItemsWithInvalidContainer(@TempDir Path tempDir) throws IOException {
        StarterKitPathLayout layout = StarterKitPathLayout.fromDataDirectory(
                tempDir.resolve("mods").resolve("starterkit-data"));
        Path kitPath = layout.resolveConfigResourcePath("StarterKit/config/kit.json");
        Files.createDirectories(kitPath.getParent());
        Files.writeString(kitPath, """
                {
                  "version": 1,
                  "enabled": true,
                  "items": [
                    { "container": "banana", "itemId": "Food_Bread", "quantity": 1 },
                    { "container": "hotbar", "itemId": "Food_Bread", "quantity": 5 }
                  ]
                }
                """);

        StarterKitConfig config = new StarterKitConfigService(layout.pluginConfigRoot()).load();

        assertThat(config.items()).hasSize(1);
        assertThat(config.items().get(0).container()).isEqualTo("hotbar");
    }

    @Test
    void loadHandlesDisabledConfig(@TempDir Path tempDir) throws IOException {
        StarterKitPathLayout layout = StarterKitPathLayout.fromDataDirectory(
                tempDir.resolve("mods").resolve("starterkit-data"));
        Path kitPath = layout.resolveConfigResourcePath("StarterKit/config/kit.json");
        Files.createDirectories(kitPath.getParent());
        Files.writeString(kitPath, """
                {
                  "version": 1,
                  "enabled": false,
                  "items": []
                }
                """);

        StarterKitConfig config = new StarterKitConfigService(layout.pluginConfigRoot()).load();

        assertThat(config.enabled()).isFalse();
    }

    @Test
    void loadHandlesMissingItemsKey(@TempDir Path tempDir) throws IOException {
        StarterKitPathLayout layout = StarterKitPathLayout.fromDataDirectory(
                tempDir.resolve("mods").resolve("starterkit-data"));
        Path kitPath = layout.resolveConfigResourcePath("StarterKit/config/kit.json");
        Files.createDirectories(kitPath.getParent());
        Files.writeString(kitPath, """
                { "version": 1, "enabled": true }
                """);

        StarterKitConfig config = new StarterKitConfigService(layout.pluginConfigRoot()).load();

        assertThat(config.enabled()).isTrue();
        assertThat(config.items()).isEmpty();
    }
}
```

- [ ] **Step 8: Implement StarterKitConfigService**

Create `plugins/starterkit/src/main/java/org/runetale/starterkit/config/StarterKitConfigService.java`:

```java
package org.runetale.starterkit.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import org.runetale.starterkit.domain.KitItem;
import org.runetale.starterkit.domain.StarterKitConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StarterKitConfigService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String KIT_CONFIG_RESOURCE = "StarterKit/config/kit.json";

    @Nonnull
    private final Path configRoot;

    public StarterKitConfigService(@Nonnull Path configRoot) {
        this.configRoot = configRoot;
    }

    @Nonnull
    public StarterKitConfig load() {
        Path configPath = this.configRoot.resolve(
                StarterKitPathLayout.externalRelativeResourcePath(KIT_CONFIG_RESOURCE));
        try (InputStream input = openInput(configPath)) {
            if (input == null) {
                LOGGER.atWarning().log(
                        "[StarterKit] Config missing at path=%s and classpath fallback unavailable", configPath);
                return StarterKitConfig.defaults();
            }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    LOGGER.atWarning().log("[StarterKit] Config root must be object path=%s", configPath);
                    return StarterKitConfig.defaults();
                }
                StarterKitConfig config = parseConfig(parsed.getAsJsonObject());
                LOGGER.atInfo().log("[StarterKit] Loaded config items=%d enabled=%s path=%s",
                        config.items().size(), config.enabled(), configPath);
                return config;
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[StarterKit] Failed loading config path=%s", configPath);
            return StarterKitConfig.defaults();
        }
    }

    @Nullable
    private InputStream openInput(@Nonnull Path configPath) throws IOException {
        if (Files.isRegularFile(configPath)) {
            return Files.newInputStream(configPath);
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader serviceClassLoader = StarterKitConfigService.class.getClassLoader();
        ClassLoader classLoader = serviceClassLoader != null ? serviceClassLoader : contextClassLoader;
        if (classLoader == null) {
            return null;
        }

        InputStream selectedInput = classLoader.getResourceAsStream(KIT_CONFIG_RESOURCE);
        if (selectedInput == null && contextClassLoader != null && contextClassLoader != classLoader) {
            selectedInput = contextClassLoader.getResourceAsStream(KIT_CONFIG_RESOURCE);
        }
        return selectedInput;
    }

    @Nonnull
    private StarterKitConfig parseConfig(@Nonnull JsonObject root) {
        int version = intValue(root, "version", StarterKitConfig.DEFAULT_VERSION);
        boolean enabled = booleanValue(root, "enabled", true);
        List<KitItem> items = parseItems(root);
        return new StarterKitConfig(Math.max(1, version), enabled, List.copyOf(items));
    }

    @Nonnull
    private List<KitItem> parseItems(@Nonnull JsonObject root) {
        JsonArray itemsArray = arrayValue(root, "items");
        if (itemsArray == null) {
            return List.of();
        }

        List<KitItem> items = new ArrayList<>();
        int index = 0;
        for (JsonElement element : itemsArray) {
            if (!element.isJsonObject()) {
                index++;
                continue;
            }
            KitItem parsed = parseItem(element.getAsJsonObject(), index);
            if (parsed != null) {
                items.add(parsed);
            }
            index++;
        }
        return items;
    }

    @Nullable
    private KitItem parseItem(@Nonnull JsonObject object, int index) {
        String container = stringValue(object, "container", "");
        String itemId = stringValue(object, "itemId", "");
        int quantity = intValue(object, "quantity", 1);

        try {
            return new KitItem(container, itemId, quantity);
        } catch (IllegalArgumentException e) {
            LOGGER.atWarning().log("[StarterKit] Skipping invalid kit item index=%d: %s", index, e.getMessage());
            return null;
        }
    }

    @Nullable
    private static JsonArray arrayValue(@Nonnull JsonObject object, @Nonnull String key) {
        JsonElement element = object.get(key);
        if (element != null && element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        return null;
    }

    @Nonnull
    private static String stringValue(@Nonnull JsonObject object, @Nonnull String key, @Nonnull String fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        String value = element.getAsString();
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return fallback;
    }

    private static int intValue(@Nonnull JsonObject object, @Nonnull String key, int fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean booleanValue(@Nonnull JsonObject object, @Nonnull String key, boolean fallback) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return element.getAsBoolean();
        } catch (Exception ignored) {
            String raw = element.getAsString().toLowerCase(Locale.ROOT);
            if (raw.equals("true") || raw.equals("false")) {
                return Boolean.parseBoolean(raw);
            }
            return fallback;
        }
    }
}
```

- [ ] **Step 9: Run all config tests**

Run: `./gradlew :plugins:starterkit:test`
Expected: all tests PASS (PathLayout + ConfigService tests).

- [ ] **Step 10: Run bootstrap contract tests**

Run: `./gradlew :plugins:starterkit:contractTest`
Expected: all contract tests PASS (bootstrap seeds and does not overwrite).

- [ ] **Step 11: Commit**

```bash
git add plugins/starterkit/src/main/java/org/runetale/starterkit/config/ \
       plugins/starterkit/src/test/java/org/runetale/starterkit/config/
git commit -m "Add StarterKit config infrastructure: PathLayout, Bootstrap, ConfigService"
```

---

### Task 4: ECS Component (ReceivedStarterKitComponent)

**Files:**
- Create: `plugins/starterkit/src/main/java/org/runetale/starterkit/component/ReceivedStarterKitComponent.java`

- [ ] **Step 1: Implement ReceivedStarterKitComponent**

Create `plugins/starterkit/src/main/java/org/runetale/starterkit/component/ReceivedStarterKitComponent.java`:

```java
package org.runetale.starterkit.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ReceivedStarterKitComponent implements Component<EntityStore> {

    public static final BuilderCodec<ReceivedStarterKitComponent> CODEC = BuilderCodec.builder(
            ReceivedStarterKitComponent.class,
            ReceivedStarterKitComponent::new)
            .append(
                    new KeyedCodec<>("GrantedAtEpochMillis", Codec.LONG),
                    (component, value) -> component.grantedAtEpochMillis = value,
                    ReceivedStarterKitComponent::getGrantedAtEpochMillis)
            .add()
            .build();

    private long grantedAtEpochMillis;

    protected ReceivedStarterKitComponent() {
        this.grantedAtEpochMillis = 0L;
    }

    public ReceivedStarterKitComponent(long grantedAtEpochMillis) {
        this.grantedAtEpochMillis = grantedAtEpochMillis;
    }

    public long getGrantedAtEpochMillis() {
        return this.grantedAtEpochMillis;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new ReceivedStarterKitComponent(this.grantedAtEpochMillis);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :plugins:starterkit:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add plugins/starterkit/src/main/java/org/runetale/starterkit/component/
git commit -m "Add ReceivedStarterKitComponent ECS marker with codec"
```

---

### Task 5: ECS System (GrantStarterKitSystem)

**Files:**
- Create: `plugins/starterkit/src/main/java/org/runetale/starterkit/system/GrantStarterKitSystem.java`

- [ ] **Step 1: Implement GrantStarterKitSystem**

Create `plugins/starterkit/src/main/java/org/runetale/starterkit/system/GrantStarterKitSystem.java`:

```java
package org.runetale.starterkit.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.starterkit.component.ReceivedStarterKitComponent;
import org.runetale.starterkit.domain.KitItem;
import org.runetale.starterkit.domain.StarterKitConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class GrantStarterKitSystem extends HolderSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ComponentType<EntityStore, ReceivedStarterKitComponent> receivedKitComponentType;
    private final StarterKitConfig config;
    private final Query<EntityStore> query;

    public GrantStarterKitSystem(
            @Nonnull ComponentType<EntityStore, ReceivedStarterKitComponent> receivedKitComponentType,
            @Nonnull StarterKitConfig config) {
        this.receivedKitComponentType = receivedKitComponentType;
        this.config = config;
        this.query = Query.and(PlayerRef.getComponentType(), Query.not(receivedKitComponentType));
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Override
    public void onEntityAdd(@Nonnull Holder<EntityStore> holder, @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store) {
        Player player = holder.getComponent(Player.getComponentType());
        if (player == null) {
            return;
        }

        if (!this.config.enabled()) {
            LOGGER.atFine().log("[StarterKit] Plugin disabled, marking player without granting items");
            markReceived(holder);
            return;
        }

        List<KitItem> items = this.config.items();
        if (items.isEmpty()) {
            LOGGER.atFine().log("[StarterKit] No items configured, marking player");
            markReceived(holder);
            return;
        }

        int granted = 0;
        Inventory inventory = player.getInventory();
        for (KitItem kitItem : items) {
            if (grantItem(player, inventory, kitItem, holder, store)) {
                granted++;
            }
        }

        markReceived(holder);
        LOGGER.atInfo().log("[StarterKit] Granted starter kit to player items=%d/%d", granted, items.size());
    }

    @Override
    public void onEntityRemoved(@Nonnull Holder<EntityStore> holder, @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store) {
        // No cleanup required. Component lifecycle is handled by ECS persistence.
    }

    private boolean grantItem(
            @Nonnull Player player,
            @Nonnull Inventory inventory,
            @Nonnull KitItem kitItem,
            @Nonnull Holder<EntityStore> holder,
            @Nonnull Store<EntityStore> store) {
        Item item = Item.getAssetMap().getAsset(kitItem.itemId());
        if (item == null) {
            LOGGER.atWarning().log("[StarterKit] Unknown item id=%s, skipping", kitItem.itemId());
            return false;
        }

        ItemContainer container = resolveContainer(inventory, kitItem.container());
        if (container == null) {
            LOGGER.atWarning().log("[StarterKit] Could not resolve container=%s, skipping item=%s",
                    kitItem.container(), kitItem.itemId());
            return false;
        }

        for (short slot = 0; slot < container.getCapacity(); slot++) {
            ItemStack existing = container.getItemStack(slot);
            if (existing == null || ItemStack.isEmpty(existing)) {
                container.setItemStackForSlot(slot, new ItemStack(item, kitItem.quantity()));
                return true;
            }
        }

        LOGGER.atWarning().log("[StarterKit] Container=%s full, could not place item=%s",
                kitItem.container(), kitItem.itemId());
        return false;
    }

    @Nullable
    private static ItemContainer resolveContainer(@Nonnull Inventory inventory, @Nonnull String containerName) {
        return switch (containerName) {
            case "hotbar" -> inventory.getHotbar();
            case "armour" -> inventory.getArmor();
            case "tools" -> inventory.getTools();
            case "storage" -> inventory.getStorage();
            case "backpack" -> inventory.getBackpack();
            case "utility" -> {
                // Utility may be exposed under a different accessor name.
                // Try getSectionById as fallback if getUtility does not exist.
                try {
                    yield inventory.getUtility();
                } catch (NoSuchMethodError e) {
                    LOGGER.atWarning().log("[StarterKit] Utility container not available via getUtility()");
                    yield null;
                }
            }
            default -> null;
        };
    }

    private void markReceived(@Nonnull Holder<EntityStore> holder) {
        ReceivedStarterKitComponent marker = new ReceivedStarterKitComponent(System.currentTimeMillis());
        holder.setComponent(this.receivedKitComponentType, marker);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :plugins:starterkit:compileJava`
Expected: BUILD SUCCESSFUL. If `getUtility()` or `setItemStackForSlot()` don't exist on the API, compilation will reveal the exact method names needed — adjust accordingly.

- [ ] **Step 3: Commit**

```bash
git add plugins/starterkit/src/main/java/org/runetale/starterkit/system/
git commit -m "Add GrantStarterKitSystem ECS system for first-time player item granting"
```

---

### Task 6: Plugin Entry Point (StarterKitPlugin)

**Files:**
- Create: `plugins/starterkit/src/main/java/org/runetale/starterkit/StarterKitPlugin.java`

- [ ] **Step 1: Implement StarterKitPlugin**

Create `plugins/starterkit/src/main/java/org/runetale/starterkit/StarterKitPlugin.java`:

```java
package org.runetale.starterkit;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.starterkit.component.ReceivedStarterKitComponent;
import org.runetale.starterkit.config.StarterKitConfigService;
import org.runetale.starterkit.config.StarterKitExternalConfigBootstrap;
import org.runetale.starterkit.config.StarterKitPathLayout;
import org.runetale.starterkit.domain.StarterKitConfig;
import org.runetale.starterkit.system.GrantStarterKitSystem;

import javax.annotation.Nonnull;

public class StarterKitPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private StarterKitConfig config;
    private ComponentType<EntityStore, ReceivedStarterKitComponent> receivedKitComponentType;

    public StarterKitPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up starter kit plugin...");

        StarterKitPathLayout pathLayout = StarterKitPathLayout.fromDataDirectory(this.getDataDirectory());
        StarterKitExternalConfigBootstrap.seedMissingDefaults(pathLayout);

        StarterKitConfigService configService = new StarterKitConfigService(pathLayout.pluginConfigRoot());
        this.config = configService.load();

        this.receivedKitComponentType = this.getEntityStoreRegistry()
                .registerComponent(ReceivedStarterKitComponent.class, "ReceivedStarterKit",
                        ReceivedStarterKitComponent.CODEC);

        this.getEntityStoreRegistry()
                .registerSystem(new GrantStarterKitSystem(this.receivedKitComponentType, this.config));

        LOGGER.atInfo().log("Starter kit setup complete enabled=%s items=%d",
                this.config.enabled(), this.config.items().size());
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started starter kit plugin.");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down starter kit plugin...");
        this.config = null;
        this.receivedKitComponentType = null;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :plugins:starterkit:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add plugins/starterkit/src/main/java/org/runetale/starterkit/StarterKitPlugin.java
git commit -m "Add StarterKitPlugin entry point wiring config, component, and system"
```

---

### Task 7: Manifest Contract Test

**Files:**
- Create: `plugins/starterkit/src/test/java/org/runetale/starterkit/manifest/StarterKitManifestContractTest.java`

- [ ] **Step 1: Write manifest contract test**

Create `plugins/starterkit/src/test/java/org/runetale/starterkit/manifest/StarterKitManifestContractTest.java`:

```java
package org.runetale.starterkit.manifest;

import org.junit.jupiter.api.Test;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class StarterKitManifestContractTest {

    @Test
    void manifestDeclaresCorrectMainClass() throws IOException {
        String manifest = readManifest();

        assertThat(manifest).contains("\"Name\": \"StarterKitPlugin\"");
        assertThat(manifest).contains("\"Main\": \"org.runetale.starterkit.StarterKitPlugin\"");
    }

    @Test
    void manifestDeclaresNoDependencies() throws IOException {
        String manifest = readManifest();

        assertThat(manifest).doesNotContain("Dependencies");
        assertThat(manifest).doesNotContain("SkillsPlugin");
        assertThat(manifest).doesNotContain("Hybrid");
    }

    private static String readManifest() throws IOException {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("manifest.json")) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
```

- [ ] **Step 2: Run all tests**

Run: `./gradlew :plugins:starterkit:test :plugins:starterkit:contractTest`
Expected: all tests PASS.

- [ ] **Step 3: Commit**

```bash
git add plugins/starterkit/src/test/java/org/runetale/starterkit/manifest/
git commit -m "Add StarterKit manifest contract test"
```

---

### Task 8: Build Integration (deploy tasks)

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Add starterkit config deploy task and wire into deployPluginsToRun**

In `build.gradle.kts`, add the starterkit config resource dirs and deploy task alongside the existing ones (after the loot-protection entries around line 221), and update `deployPluginsToRun` to include it.

Add after the `lootProtectionConfigRunDir` line (line 221):

```kotlin
val starterKitConfigResourceDirs = listOf(
    layout.projectDirectory.dir("plugins/starterkit/src/main/resources/StarterKit")
)
val starterKitConfigRunDir = layout.projectDirectory.dir("server/mods/runetale/config/starterkit")
```

Add after the `deployLootProtectionConfigToRun` task (after line 248):

```kotlin
tasks.register<Sync>("deployStarterKitConfigToRun") {
    from(starterKitConfigResourceDirs)
    into(starterKitConfigRunDir)

    doLast {
        println("Synced starter kit config to ${starterKitConfigRunDir.asFile}")
    }
}
```

Add `"deployStarterKitConfigToRun"` to the `dependsOn` list in `deployPluginsToRun` (around line 296).

Add a `from(starterKitConfigResourceDirs)` block in `bundleModsRelease` with `into("runetale/config/starterkit")`.

- [ ] **Step 2: Verify deploy task works**

Run: `./gradlew :plugins:starterkit:build`
Expected: BUILD SUCCESSFUL, produces `runetale-starterkit.jar`.

- [ ] **Step 3: Run full build to check nothing is broken**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL across all modules.

- [ ] **Step 4: Commit**

```bash
git add build.gradle.kts
git commit -m "Wire starterkit plugin into build deploy and bundle tasks"
```

---

### Task 9: Final Verification

- [ ] **Step 1: Run all starterkit tests**

Run: `./gradlew :plugins:starterkit:test :plugins:starterkit:contractTest`
Expected: all tests PASS.

- [ ] **Step 2: Run full project verification**

Run: `./gradlew build verifyTests`
Expected: BUILD SUCCESSFUL, all modules pass.

- [ ] **Step 3: Verify JAR output**

Run: `ls -la plugins/starterkit/build/libs/runetale-starterkit*.jar`
Expected: `runetale-starterkit.jar` exists.

- [ ] **Step 4: Commit any remaining changes**

If any fixes were needed during verification, commit them.

---

## Manual In-Game Verification (Post-Implementation)

These cannot be automated and require human testing:

1. Deploy to local server: `./gradlew deployPluginsToRun`
2. Join as a new player — verify starter items appear in inventory
3. Disconnect and rejoin — verify no duplicate kit (component persisted)
4. Edit `kit.json` to change items, restart server — verify new config for new players
5. Set `"enabled": false` in config — verify new players get no items but component is still attached
