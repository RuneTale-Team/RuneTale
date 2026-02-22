package org.runetale.blockregeneration.domain;

import javax.annotation.Nonnull;

public record BlockRegenDefinition(
        @Nonnull String id,
        boolean enabled,
        @Nonnull String blockIdPattern,
        @Nonnull String interactedBlockId,
        @Nonnull GatheringTrigger gatheringTrigger,
        @Nonnull RespawnDelay respawnDelay) {
}
