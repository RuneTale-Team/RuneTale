package org.runetale.blockregeneration.service;

import org.junit.jupiter.api.Test;
import org.runetale.blockregeneration.domain.BlockRegenConfig;
import org.runetale.blockregeneration.domain.BlockRegenDefinition;
import org.runetale.blockregeneration.domain.GatheringTrigger;
import org.runetale.blockregeneration.domain.RespawnDelay;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BlockRegenDefinitionServiceTest {

    @Test
    void exactMappingWinsOverWildcard() {
        BlockRegenDefinition wildcard = definition("wildcard", "Ore_Iron_*");
        BlockRegenDefinition exact = definition("exact", "Ore_Iron_A");
        BlockRegenConfig config = new BlockRegenConfig(1, true, 500L, 1500L, List.of(wildcard, exact));
        BlockRegenDefinitionService service = new BlockRegenDefinitionService();

        service.load(config);

        assertThat(service.findByBlockId("Ore_Iron_A")).isEqualTo(exact);
        assertThat(service.findByBlockId("Ore_Iron_B")).isEqualTo(wildcard);
    }

    @Test
    void wildcardMatchesSimplifiedNamespacedBlockId() {
        BlockRegenDefinition wildcard = definition("wildcard", "ore_iron_*");
        BlockRegenConfig config = new BlockRegenConfig(1, true, 500L, 1500L, List.of(wildcard));
        BlockRegenDefinitionService service = new BlockRegenDefinitionService();

        service.load(config);

        assertThat(service.findByBlockId("mymod:mining/Ore_Iron_A")).isEqualTo(wildcard);
    }

    @Test
    void interactedBlockLookupMatchesExactId() {
        BlockRegenDefinition wildcard = definition("wildcard", "Ore_Iron_*");
        BlockRegenConfig config = new BlockRegenConfig(1, true, 500L, 1500L, List.of(wildcard));
        BlockRegenDefinitionService service = new BlockRegenDefinitionService();

        service.load(config);

        assertThat(service.findByInteractedBlockId("Empty_Ore_Vein")).isEqualTo(wildcard);
    }

    private static BlockRegenDefinition definition(String id, String pattern) {
        return new BlockRegenDefinition(
                id,
                true,
                pattern,
                "Empty_Ore_Vein",
                new GatheringTrigger(GatheringTrigger.Type.SPECIFIC, 1, 1, 1),
                new RespawnDelay(RespawnDelay.Type.SET, 5000L, 5000L, 5000L));
    }
}
