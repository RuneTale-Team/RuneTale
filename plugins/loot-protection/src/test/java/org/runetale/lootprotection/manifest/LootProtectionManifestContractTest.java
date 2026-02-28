package org.runetale.lootprotection.manifest;

import org.junit.jupiter.api.Test;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class LootProtectionManifestContractTest {

    @Test
    void manifestDeclaresStandalonePlugin() throws IOException {
        String manifest = readManifest();

        assertThat(manifest).contains("\"Name\": \"LootProtectionPlugin\"");
        assertThat(manifest).contains("\"Main\": \"org.runetale.lootprotection.LootProtectionPlugin\"");
        assertThat(manifest).doesNotContain("SkillsPlugin");
        assertThat(manifest).doesNotContain("SkillsApiPlugin");
    }

    private static String readManifest() throws IOException {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("manifest.json")) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
