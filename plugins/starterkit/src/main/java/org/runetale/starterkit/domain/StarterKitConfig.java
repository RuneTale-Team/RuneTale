package org.runetale.starterkit.domain;

import javax.annotation.Nonnull;
import java.util.List;

public record StarterKitConfig(int version, boolean enabled, @Nonnull List<KitItem> items) {

    public static final int DEFAULT_VERSION = 1;

    public StarterKitConfig {
        if (items == null) {
            items = List.of();
        }
    }

    @Nonnull
    public static StarterKitConfig defaults() {
        return new StarterKitConfig(DEFAULT_VERSION, true, List.of());
    }
}
