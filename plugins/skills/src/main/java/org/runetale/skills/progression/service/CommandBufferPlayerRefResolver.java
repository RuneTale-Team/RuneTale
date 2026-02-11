package org.runetale.skills.progression.service;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Resolves {@link PlayerRef} through the ECS command buffer component lookup.
 */
public class CommandBufferPlayerRefResolver implements PlayerRefResolver {

	@Nullable
	@Override
	public PlayerRef resolve(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> ref) {
		return commandBuffer.getComponent(ref, PlayerRef.getComponentType());
	}
}
