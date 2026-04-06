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
