package org.runetale.blockregeneration.domain;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Random;

public record GatheringTrigger(
        @Nonnull Type type,
        int amount,
        int amountMin,
        int amountMax) {

    public enum Type {
        SPECIFIC,
        RANDOM;

        @Nonnull
        public static Type parse(@Nonnull String raw, @Nonnull Type fallback) {
            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            if (normalized.equals("SPECIFIC")) {
                return SPECIFIC;
            }
            if (normalized.equals("RANDOM")) {
                return RANDOM;
            }
            return fallback;
        }
    }

    public int sampleThreshold(@Nonnull Random random) {
        if (this.type == Type.SPECIFIC) {
            return Math.max(1, this.amount);
        }
        int min = Math.max(1, Math.min(this.amountMin, this.amountMax));
        int max = Math.max(min, Math.max(this.amountMin, this.amountMax));
        if (max == min) {
            return min;
        }
        return random.nextInt(max - min + 1) + min;
    }
}
