package org.runetale.lootprotection.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlockOwnershipClaimService {

    private final Map<BlockKey, Claim> claimsByBlock = new ConcurrentHashMap<>();

    @Nonnull
    public ClaimResult claimOrRefresh(
            @Nonnull String worldName,
            int x,
            int y,
            int z,
            @Nonnull UUID actorPlayerId,
            long nowMillis,
            long inactivityResetMillis) {
        BlockKey key = new BlockKey(worldName, x, y, z);
        long inactivityWindow = Math.max(1L, inactivityResetMillis);

        ClaimResult[] holder = new ClaimResult[1];
        this.claimsByBlock.compute(key, (ignored, existing) -> {
            if (existing == null || existing.isExpired(nowMillis, inactivityWindow)) {
                holder[0] = new ClaimResult(ClaimStatus.ACQUIRED, actorPlayerId, null);
                return new Claim(actorPlayerId, nowMillis, nowMillis);
            }

            if (existing.ownerPlayerId().equals(actorPlayerId)) {
                Claim refreshed = new Claim(existing.ownerPlayerId(), existing.claimedAtEpochMillis(), nowMillis);
                holder[0] = new ClaimResult(ClaimStatus.REFRESHED, actorPlayerId, null);
                return refreshed;
            }

            holder[0] = new ClaimResult(ClaimStatus.BLOCKED_BY_OTHER, actorPlayerId, existing.ownerPlayerId());
            return existing;
        });

        return holder[0];
    }

    public void clear(@Nonnull String worldName, int x, int y, int z) {
        this.claimsByBlock.remove(new BlockKey(worldName, x, y, z));
    }

    public void clear() {
        this.claimsByBlock.clear();
    }

    public enum ClaimStatus {
        ACQUIRED,
        REFRESHED,
        BLOCKED_BY_OTHER
    }

    public record ClaimResult(
            @Nonnull ClaimStatus status,
            @Nonnull UUID actorPlayerId,
            @Nullable UUID blockingPlayerId) {

        public boolean isBlocked() {
            return this.status == ClaimStatus.BLOCKED_BY_OTHER;
        }
    }

    private record BlockKey(
            @Nonnull String worldName,
            int x,
            int y,
            int z) {
    }

    private record Claim(
            @Nonnull UUID ownerPlayerId,
            long claimedAtEpochMillis,
            long lastActivityAtEpochMillis) {

        boolean isExpired(long nowMillis, long inactivityResetMillis) {
            return nowMillis - this.lastActivityAtEpochMillis >= inactivityResetMillis;
        }
    }
}
