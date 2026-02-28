package org.runetale.lootprotection.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class DropClaimWindowService {

    private final List<DropClaimWindow> windows = new CopyOnWriteArrayList<>();

    public void openWindow(
            @Nonnull String worldName,
            double x,
            double y,
            double z,
            @Nonnull UUID ownerPlayerId,
            long nowMillis,
            long windowMillis) {
        long duration = Math.max(1L, windowMillis);
        this.windows.add(new DropClaimWindow(
                worldName,
                x,
                y,
                z,
                ownerPlayerId,
                nowMillis,
                nowMillis + duration));
    }

    @Nullable
    public UUID findOwnerForDrop(
            @Nonnull String worldName,
            double x,
            double y,
            double z,
            long nowMillis,
            double matchRadius) {
        pruneExpired(nowMillis);
        double maxDistance = Math.max(0.0D, matchRadius);
        double maxDistanceSquared = maxDistance * maxDistance;

        DropClaimWindow best = null;
        double bestDistanceSquared = Double.MAX_VALUE;
        for (DropClaimWindow window : this.windows) {
            if (!window.worldName().equals(worldName)) {
                continue;
            }
            if (window.expiresAtEpochMillis() < nowMillis) {
                continue;
            }

            double dx = x - window.x();
            double dy = y - window.y();
            double dz = z - window.z();
            double distanceSquared = (dx * dx) + (dy * dy) + (dz * dz);
            if (distanceSquared > maxDistanceSquared) {
                continue;
            }

            if (best == null || distanceSquared < bestDistanceSquared ||
                    (distanceSquared == bestDistanceSquared && window.createdAtEpochMillis() > best.createdAtEpochMillis())) {
                best = window;
                bestDistanceSquared = distanceSquared;
            }
        }

        return best == null ? null : best.ownerPlayerId();
    }

    public void pruneExpired(long nowMillis) {
        List<DropClaimWindow> expired = new ArrayList<>();
        for (DropClaimWindow window : this.windows) {
            if (window.expiresAtEpochMillis() < nowMillis) {
                expired.add(window);
            }
        }
        if (!expired.isEmpty()) {
            this.windows.removeAll(expired);
        }
    }

    public void clear() {
        this.windows.clear();
    }

    private record DropClaimWindow(
            @Nonnull String worldName,
            double x,
            double y,
            double z,
            @Nonnull UUID ownerPlayerId,
            long createdAtEpochMillis,
            long expiresAtEpochMillis) {
    }
}
