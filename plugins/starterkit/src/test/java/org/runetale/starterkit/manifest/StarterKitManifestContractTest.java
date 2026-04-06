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
