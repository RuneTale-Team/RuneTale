package org.runetale.starterkit.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ReceivedStarterKitComponent implements Component<EntityStore> {

    public static final BuilderCodec<ReceivedStarterKitComponent> CODEC = BuilderCodec.builder(
            ReceivedStarterKitComponent.class,
            ReceivedStarterKitComponent::new)
            .append(
                    new KeyedCodec<>("GrantedAtEpochMillis", Codec.LONG),
                    (component, value) -> component.grantedAtEpochMillis = value,
                    ReceivedStarterKitComponent::getGrantedAtEpochMillis)
            .add()
            .build();

    private long grantedAtEpochMillis;

    protected ReceivedStarterKitComponent() {
        this.grantedAtEpochMillis = 0L;
    }

    public ReceivedStarterKitComponent(long grantedAtEpochMillis) {
        this.grantedAtEpochMillis = grantedAtEpochMillis;
    }

    public long getGrantedAtEpochMillis() {
        return this.grantedAtEpochMillis;
    }

    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new ReceivedStarterKitComponent(this.grantedAtEpochMillis);
    }
}
