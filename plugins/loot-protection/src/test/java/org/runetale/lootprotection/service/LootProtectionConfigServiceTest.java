package org.runetale.lootprotection.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.runetale.lootprotection.config.LootProtectionConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LootProtectionConfigServiceTest {

    @Test
    void loadFallsBackToDefaultsWhenConfigMissing(@TempDir Path tempDir) {
        LootProtectionConfigService service = new LootProtectionConfigService(tempDir);

        LootProtectionConfig config = service.load();

        assertThat(config.enabled()).isTrue();
        assertThat(config.protectBlockBreakDrops()).isTrue();
        assertThat(config.protectKillDrops()).isTrue();
    }

    @Test
    void loadNormalizesInvalidNumericValues(@TempDir Path tempDir) throws IOException {
        Path configPath = tempDir.resolve("config").resolve("loot-protection.json");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, """
                {
                  "enabled": true,
                  "blockOwnership": {
                    "inactivityResetMillis": -1,
                    "notifyCooldownMillis": 0
                  },
                  "dropClaim": {
                    "windowMillis": 0,
                    "matchRadius": 0.0
                  },
                  "ownerLock": {
                    "enabled": true,
                    "timeoutMillis": 10,
                    "retryIntervalMillis": 10,
                    "inventoryFullNotifyCooldownMillis": 1
                  }
                }
                """);

        LootProtectionConfigService service = new LootProtectionConfigService(tempDir);
        LootProtectionConfig config = service.load();

        assertThat(config.blockOwnership().inactivityResetMillis()).isEqualTo(250L);
        assertThat(config.blockOwnership().notifyCooldownMillis()).isEqualTo(100L);
        assertThat(config.dropClaim().windowMillis()).isEqualTo(100L);
        assertThat(config.dropClaim().matchRadius()).isEqualTo(0.5D);
        assertThat(config.ownerLock().timeoutMillis()).isEqualTo(1_000L);
        assertThat(config.ownerLock().retryIntervalMillis()).isEqualTo(50L);
        assertThat(config.ownerLock().inventoryFullNotifyCooldownMillis()).isEqualTo(100L);
    }
}
