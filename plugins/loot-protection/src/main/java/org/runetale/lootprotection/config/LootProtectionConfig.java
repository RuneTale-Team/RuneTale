package org.runetale.lootprotection.config;

import javax.annotation.Nonnull;

public record LootProtectionConfig(
        boolean enabled,
        boolean protectBlockBreakDrops,
        boolean protectKillDrops,
        @Nonnull BlockOwnership blockOwnership,
        @Nonnull DropClaim dropClaim,
        @Nonnull OwnerLock ownerLock) {

    public static LootProtectionConfig defaults() {
        return new LootProtectionConfig(
                true,
                true,
                true,
                BlockOwnership.defaults(),
                DropClaim.defaults(),
                OwnerLock.defaults());
    }

    public record BlockOwnership(
            boolean enabled,
            long inactivityResetMillis,
            long notifyCooldownMillis) {

        public static BlockOwnership defaults() {
            return new BlockOwnership(true, 3000L, 1500L);
        }

        public BlockOwnership normalized() {
            return new BlockOwnership(
                    this.enabled,
                    Math.max(250L, this.inactivityResetMillis),
                    Math.max(100L, this.notifyCooldownMillis));
        }
    }

    public record DropClaim(
            long windowMillis,
            double matchRadius) {

        public static DropClaim defaults() {
            return new DropClaim(800L, 3.0D);
        }

        public DropClaim normalized() {
            return new DropClaim(
                    Math.max(100L, this.windowMillis),
                    Math.max(0.5D, this.matchRadius));
        }
    }

    public record OwnerLock(
            boolean enabled,
            long timeoutMillis,
            long retryIntervalMillis,
            long inventoryFullNotifyCooldownMillis) {

        public static OwnerLock defaults() {
            return new OwnerLock(true, 15_000L, 250L, 2_000L);
        }

        public OwnerLock normalized() {
            return new OwnerLock(
                    this.enabled,
                    Math.max(1000L, this.timeoutMillis),
                    Math.max(50L, this.retryIntervalMillis),
                    Math.max(100L, this.inventoryFullNotifyCooldownMillis));
        }
    }

    public LootProtectionConfig normalized() {
        return new LootProtectionConfig(
                this.enabled,
                this.protectBlockBreakDrops,
                this.protectKillDrops,
                this.blockOwnership.normalized(),
                this.dropClaim.normalized(),
                this.ownerLock.normalized());
    }
}
