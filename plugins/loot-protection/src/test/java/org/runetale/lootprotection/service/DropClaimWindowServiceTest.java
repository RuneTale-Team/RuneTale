package org.runetale.lootprotection.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DropClaimWindowServiceTest {

    @Test
    void multiDropPositionsWithinWindowMatchOwner() {
        DropClaimWindowService service = new DropClaimWindowService();
        UUID owner = UUID.randomUUID();

        service.openWindow("world", 100.5D, 65.0D, 200.5D, owner, 1_000L, 800L);

        UUID first = service.findOwnerForDrop("world", 100.2D, 65.1D, 200.6D, 1_050L, 3.0D);
        UUID second = service.findOwnerForDrop("world", 101.0D, 65.2D, 201.1D, 1_100L, 3.0D);
        UUID third = service.findOwnerForDrop("world", 99.7D, 64.9D, 200.0D, 1_200L, 3.0D);

        assertThat(first).isEqualTo(owner);
        assertThat(second).isEqualTo(owner);
        assertThat(third).isEqualTo(owner);
    }

    @Test
    void expiredWindowsNoLongerMatchDrops() {
        DropClaimWindowService service = new DropClaimWindowService();
        UUID owner = UUID.randomUUID();

        service.openWindow("world", 10.0D, 20.0D, 30.0D, owner, 1_000L, 500L);

        UUID duringWindow = service.findOwnerForDrop("world", 10.1D, 20.0D, 29.9D, 1_300L, 2.0D);
        UUID afterWindow = service.findOwnerForDrop("world", 10.1D, 20.0D, 29.9D, 1_600L, 2.0D);

        assertThat(duringWindow).isEqualTo(owner);
        assertThat(afterWindow).isNull();
    }
}
