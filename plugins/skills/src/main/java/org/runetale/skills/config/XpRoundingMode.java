package org.runetale.skills.config;

import javax.annotation.Nonnull;
import java.util.Locale;

public enum XpRoundingMode {
    NEAREST,
    FLOOR,
    CEIL;

    @Nonnull
    public static XpRoundingMode fromConfig(@Nonnull String raw) {
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return XpRoundingMode.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return NEAREST;
        }
    }
}
