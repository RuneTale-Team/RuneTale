package org.runetale.lootprotection.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.lootprotection.config.LootProtectionConfig;
import org.runetale.lootprotection.service.DropClaimWindowService;

import javax.annotation.Nonnull;

public class LootProtectionDeathDropClaimSystem extends DeathSystems.OnDeathSystem {

    private final LootProtectionConfig config;
    private final DropClaimWindowService dropClaimWindowService;

    public LootProtectionDeathDropClaimSystem(
            @Nonnull LootProtectionConfig config,
            @Nonnull DropClaimWindowService dropClaimWindowService) {
        this.config = config;
        this.dropClaimWindowService = dropClaimWindowService;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull DeathComponent component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (!this.config.enabled() || !this.config.protectKillDrops() || !this.config.ownerLock().enabled()) {
            return;
        }

        if (store.getArchetype(ref).contains(Player.getComponentType())) {
            return;
        }

        Damage deathInfo = component.getDeathInfo();
        if (deathInfo == null || !(deathInfo.getSource() instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> sourceRef = entitySource.getRef();
        if (!sourceRef.isValid()) {
            return;
        }

        PlayerRef killerPlayerRef = commandBuffer.getComponent(sourceRef, PlayerRef.getComponentType());
        if (killerPlayerRef == null) {
            killerPlayerRef = store.getComponent(sourceRef, PlayerRef.getComponentType());
        }
        if (killerPlayerRef == null) {
            return;
        }

        TransformComponent transformComponent = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent == null) {
            transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        }
        if (transformComponent == null) {
            return;
        }

        long now = System.currentTimeMillis();
        this.dropClaimWindowService.openWindow(
                store.getExternalData().getWorld().getName(),
                transformComponent.getPosition().getX(),
                transformComponent.getPosition().getY(),
                transformComponent.getPosition().getZ(),
                killerPlayerRef.getUuid(),
                now,
                this.config.dropClaim().windowMillis());
    }
}
