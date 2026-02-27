package org.runetale.skills.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.event.SkillLevelUpEvent;
import org.runetale.skills.service.ConstitutionHealthService;
import org.runetale.testing.junit.ContractTest;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ContractTest
class ConstitutionHealthLevelUpSystemContractTest {

	@Test
	void handleAppliesHealthSyncForConstitutionLevelUps() {
		ConstitutionHealthService healthService = mock(ConstitutionHealthService.class);
		ConstitutionHealthLevelUpSystem system = new ConstitutionHealthLevelUpSystem(healthService);

		ArchetypeChunk<EntityStore> chunk = mock(ArchetypeChunk.class);
		Store<EntityStore> store = mock(Store.class);
		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);
		Ref<EntityStore> playerRef = mock(Ref.class);

		when(chunk.getReferenceTo(0)).thenReturn(playerRef);

		system.handle(0, chunk, store, commandBuffer, new SkillLevelUpEvent(SkillType.CONSTITUTION, 10, 11));

		verify(healthService).syncForLevel(eq(commandBuffer), eq(playerRef), eq(11), eq(true));
	}

	@Test
	void handleIgnoresNonConstitutionLevelUps() {
		ConstitutionHealthService healthService = mock(ConstitutionHealthService.class);
		ConstitutionHealthLevelUpSystem system = new ConstitutionHealthLevelUpSystem(healthService);

		ArchetypeChunk<EntityStore> chunk = mock(ArchetypeChunk.class);
		Store<EntityStore> store = mock(Store.class);
		CommandBuffer<EntityStore> commandBuffer = mock(CommandBuffer.class);

		system.handle(0, chunk, store, commandBuffer, new SkillLevelUpEvent(SkillType.ATTACK, 10, 11));

		verifyNoInteractions(healthService);
	}
}
