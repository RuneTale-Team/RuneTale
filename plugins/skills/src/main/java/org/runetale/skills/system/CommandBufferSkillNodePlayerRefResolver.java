package org.runetale.skills.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Command-buffer-backed player reference resolver for node-break handling.
 */
public class CommandBufferSkillNodePlayerRefResolver implements SkillNodePlayerRefResolver {

	@Nullable
	@Override
	public PlayerRef resolve(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> ref) {
		return commandBuffer.getComponent(ref, PlayerRef.getComponentType());
	}
}
