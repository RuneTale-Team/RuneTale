package org.runetale.lootprotection.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class LootProtectionExternalConfigBootstrapContractTest {

    @Test
    void seedMissingDefaultsCopiesLootProtectionConfig(@TempDir Path tempDir) {
        LootProtectionPathLayout layout = LootProtectionPathLayout
                .fromDataDirectory(tempDir.resolve("mods").resolve("loot-protection-data"));

        LootProtectionExternalConfigBootstrap.seedMissingDefaults(layout);

        assertThat(layout.resolveConfigResourcePath("LootProtection/config/loot-protection.json")).exists();
    }

    @Test
    void seedMissingDefaultsDoesNotOverwriteExistingFiles(@TempDir Path tempDir) throws IOException {
        LootProtectionPathLayout layout = LootProtectionPathLayout
                .fromDataDirectory(tempDir.resolve("mods").resolve("loot-protection-data"));
        Path configPath = layout.resolveConfigResourcePath("LootProtection/config/loot-protection.json");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, "{\"enabled\":false}\n");

        LootProtectionExternalConfigBootstrap.seedMissingDefaults(layout);

        assertThat(Files.readString(configPath)).isEqualTo("{\"enabled\":false}\n");
    }
}
