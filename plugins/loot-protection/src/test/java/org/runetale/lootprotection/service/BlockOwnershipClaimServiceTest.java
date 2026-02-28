package org.runetale.lootprotection.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BlockOwnershipClaimServiceTest {

    @Test
    void samePlayerRefreshesClaimWithinInactivityWindow() {
        BlockOwnershipClaimService service = new BlockOwnershipClaimService();
        UUID playerId = UUID.randomUUID();

        BlockOwnershipClaimService.ClaimResult first = service.claimOrRefresh(
                "world",
                10,
                64,
                10,
                playerId,
                1_000L,
                3_000L);
        BlockOwnershipClaimService.ClaimResult second = service.claimOrRefresh(
                "world",
                10,
                64,
                10,
                playerId,
                1_500L,
                3_000L);

        assertThat(first.status()).isEqualTo(BlockOwnershipClaimService.ClaimStatus.ACQUIRED);
        assertThat(second.status()).isEqualTo(BlockOwnershipClaimService.ClaimStatus.REFRESHED);
        assertThat(second.isBlocked()).isFalse();
    }

    @Test
    void otherPlayerIsBlockedUntilClaimExpiresByInactivity() {
        BlockOwnershipClaimService service = new BlockOwnershipClaimService();
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        service.claimOrRefresh("world", 5, 70, 5, owner, 10_000L, 3_000L);

        BlockOwnershipClaimService.ClaimResult blocked = service.claimOrRefresh(
                "world",
                5,
                70,
                5,
                other,
                12_000L,
                3_000L);
        BlockOwnershipClaimService.ClaimResult acquiredAfterExpiry = service.claimOrRefresh(
                "world",
                5,
                70,
                5,
                other,
                13_500L,
                3_000L);

        assertThat(blocked.status()).isEqualTo(BlockOwnershipClaimService.ClaimStatus.BLOCKED_BY_OTHER);
        assertThat(blocked.blockingPlayerId()).isEqualTo(owner);
        assertThat(acquiredAfterExpiry.status()).isEqualTo(BlockOwnershipClaimService.ClaimStatus.ACQUIRED);
    }
}
