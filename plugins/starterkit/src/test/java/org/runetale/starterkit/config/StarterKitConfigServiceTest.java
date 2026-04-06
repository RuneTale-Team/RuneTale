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
