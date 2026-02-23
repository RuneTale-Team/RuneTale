package org.runetale.skills.combat.manifest;

import org.junit.jupiter.api.Test;
import org.runetale.testing.junit.ContractTest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class CombatManifestContractTest {

    @Test
    void manifestDeclaresCoreAndApiDependencies() throws IOException {
        String manifest = readManifest();

        assertThat(manifest).contains("\"RuneTale:SkillsApiPlugin\": \">=0.1.0\"");
        assertThat(manifest).contains("\"RuneTale:SkillsPlugin\": \">=0.1.0\"");
    }

    private static String readManifest() throws IOException {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("manifest.json")) {
            assertThat(input).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
