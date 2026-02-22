package org.runetale.blockregeneration.domain;

import javax.annotation.Nonnull;
import java.util.List;

public record BlockRegenConfig(
        int version,
        boolean enabled,
        long respawnTickMillis,
        long notifyCooldownMillis,
        @Nonnull List<BlockRegenDefinition> definitions) {

    public static final int DEFAULT_VERSION = 1;
    public static final long DEFAULT_RESPAWN_TICK_MILLIS = 500L;
    public static final long DEFAULT_NOTIFY_COOLDOWN_MILLIS = 1500L;

    @Nonnull
    public static BlockRegenConfig defaults() {
        return new BlockRegenConfig(
                DEFAULT_VERSION,
                true,
                DEFAULT_RESPAWN_TICK_MILLIS,
                DEFAULT_NOTIFY_COOLDOWN_MILLIS,
                List.of());
    }
}
