package org.runetale.lootprotection.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class OwnedLootComponent implements Component<EntityStore> {

    public static final BuilderCodec<OwnedLootComponent> CODEC = BuilderCodec.builder(
            OwnedLootComponent.class,
            OwnedLootComponent::new)
            .append(
                    new KeyedCodec<>("OwnerPlayerId", Codec.STRING),
                    (component, value) -> component.ownerPlayerId = value,
                    OwnedLootComponent::getOwnerPlayerId)
            .add()
            .append(
                    new KeyedCodec<>("LockCreatedAtEpochMillis", Codec.LONG),
                    (component, value) -> component.lockCreatedAtEpochMillis = value,
                    OwnedLootComponent::getLockCreatedAtEpochMillis)
            .add()
            .append(
                    new KeyedCodec<>("PublicUnlockAtEpochMillis", Codec.LONG),
                    (component, value) -> component.publicUnlockAtEpochMillis = value,
                    OwnedLootComponent::getPublicUnlockAtEpochMillis)
            .add()
            .append(
                    new KeyedCodec<>("LastTransferAttemptAtEpochMillis", Codec.LONG),
                    (component, value) -> component.lastTransferAttemptAtEpochMillis = value,
                    OwnedLootComponent::getLastTransferAttemptAtEpochMillis)
            .add()
            .build();

    private String ownerPlayerId;
    private long lockCreatedAtEpochMillis;
    private long publicUnlockAtEpochMillis;
    private long lastTransferAttemptAtEpochMillis;

    protected OwnedLootComponent() {
        this.ownerPlayerId = "";
    }

    public OwnedLootComponent(
            @Nonnull UUID ownerPlayerId,
            long lockCreatedAtEpochMillis,
            long publicUnlockAtEpochMillis,
            long lastTransferAttemptAtEpochMillis) {
        this.ownerPlayerId = ownerPlayerId.toString();
        this.lockCreatedAtEpochMillis = lockCreatedAtEpochMillis;
        this.publicUnlockAtEpochMillis = publicUnlockAtEpochMillis;
        this.lastTransferAttemptAtEpochMillis = lastTransferAttemptAtEpochMillis;
    }

    @Nonnull
    public String getOwnerPlayerId() {
        return this.ownerPlayerId == null ? "" : this.ownerPlayerId;
    }

    public long getLockCreatedAtEpochMillis() {
        return this.lockCreatedAtEpochMillis;
    }

    public long getPublicUnlockAtEpochMillis() {
        return this.publicUnlockAtEpochMillis;
    }

    public long getLastTransferAttemptAtEpochMillis() {
        return this.lastTransferAttemptAtEpochMillis;
    }

    public void setLastTransferAttemptAtEpochMillis(long lastTransferAttemptAtEpochMillis) {
        this.lastTransferAttemptAtEpochMillis = lastTransferAttemptAtEpochMillis;
    }

    @Nullable
    public UUID tryGetOwnerUuid() {
        String value = getOwnerPlayerId().trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        OwnedLootComponent copy = new OwnedLootComponent();
        copy.ownerPlayerId = this.ownerPlayerId;
        copy.lockCreatedAtEpochMillis = this.lockCreatedAtEpochMillis;
        copy.publicUnlockAtEpochMillis = this.publicUnlockAtEpochMillis;
        copy.lastTransferAttemptAtEpochMillis = this.lastTransferAttemptAtEpochMillis;
        return copy;
    }
}
