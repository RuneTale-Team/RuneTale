package org.runetale.starterkit.domain;

import javax.annotation.Nonnull;
import java.util.Set;

public record KitItem(@Nonnull String container, @Nonnull String itemId, int quantity) {

    private static final Set<String> VALID_CONTAINERS = Set.of(
            "hotbar", "armour", "utility", "tools", "storage", "backpack");

    public KitItem {
        if (container == null || container.isBlank() || !VALID_CONTAINERS.contains(container)) {
            throw new IllegalArgumentException(
                    "container must be one of " + VALID_CONTAINERS + " but was: '" + container + "'");
        }
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive but was: " + quantity);
        }
    }
}
