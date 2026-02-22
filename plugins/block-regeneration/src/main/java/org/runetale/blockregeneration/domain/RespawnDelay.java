package org.runetale.blockregeneration.domain;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Random;

public record RespawnDelay(
        @Nonnull Type type,
        long millis,
        long millisMin,
        long millisMax) {

    public enum Type {
        SET,
        RANDOM;

        @Nonnull
        public static Type parse(@Nonnull String raw, @Nonnull Type fallback) {
            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            if (normalized.equals("SET")) {
                return SET;
            }
            if (normalized.equals("RANDOM")) {
                return RANDOM;
            }
            return fallback;
        }
    }

    public long sampleDelayMillis(@Nonnull Random random) {
        if (this.type == Type.SET) {
            return Math.max(1L, this.millis);
        }
        long min = Math.max(1L, Math.min(this.millisMin, this.millisMax));
        long max = Math.max(min, Math.max(this.millisMin, this.millisMax));
        if (max == min) {
            return min;
        }
        long range = max - min + 1L;
        long offset = (long) Math.floor(random.nextDouble() * range);
        return min + offset;
    }
}
