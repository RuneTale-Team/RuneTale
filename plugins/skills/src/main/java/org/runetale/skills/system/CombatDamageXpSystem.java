package org.runetale.skills.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.domain.CombatStyleType;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.service.SkillXpDispatchService;
import org.runetale.skills.service.CombatStyleService;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.UUID;

/**
 * Grants combat XP from damage events.
 */
public class CombatDamageXpSystem extends DamageEventSystem {

	private static final double XP_PER_DAMAGE = 4.0D;
	private static final String SOURCE_RANGED = "combat:ranged";
	private static final String SOURCE_MELEE_PREFIX = "combat:melee:";
	private static final String SOURCE_BLOCK_DEFENSE = "combat:block:defense";

	private final SkillXpDispatchService skillXpDispatchService;
	private final CombatStyleService combatStyleService;
	private final Query<EntityStore> query;

	public CombatDamageXpSystem(
			@Nonnull SkillXpDispatchService skillXpDispatchService,
			@Nonnull CombatStyleService combatStyleService) {
		this.skillXpDispatchService = skillXpDispatchService;
		this.combatStyleService = combatStyleService;
		this.query = AllLegacyLivingEntityTypesQuery.INSTANCE;
	}

	@Override
	public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull Store<EntityStore> store,
			@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage event) {

		if (event.isCancelled()) {
			return;
		}

		double initialDamage = Math.max(0.0D, event.getInitialAmount());
		double finalDamage = Math.max(0.0D, event.getAmount());
		double preventedDamage = Math.max(0.0D, initialDamage - finalDamage);
		boolean blocked = Boolean.TRUE.equals(event.getIfPresentMetaObject(Damage.BLOCKED));

		Damage.Source source = event.getSource();
		if (!(source instanceof Damage.EntitySource entitySource)) {
			grantDefenseBlockXp(index, archetypeChunk, commandBuffer, blocked, preventedDamage);
			return;
		}

		Ref<EntityStore> attackerRef = entitySource.getRef();
		if (!attackerRef.isValid()) {
			grantDefenseBlockXp(index, archetypeChunk, commandBuffer, blocked, preventedDamage);
			return;
		}

		PlayerRef attackerPlayerRef = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
		if (attackerPlayerRef == null) {
			grantDefenseBlockXp(index, archetypeChunk, commandBuffer, blocked, preventedDamage);
			return;
		}

		if (finalDamage > 0.0D) {
			SkillType routedSkill = resolveRoutedSkill(source, attackerPlayerRef.getUuid());
			double gainedXp = finalDamage * XP_PER_DAMAGE;
			this.skillXpDispatchService.grantSkillXp(
					commandBuffer,
					attackerRef,
					routedSkill,
					gainedXp,
					sourceTag(source, routedSkill),
					true);
		}

		grantDefenseBlockXp(index, archetypeChunk, commandBuffer, blocked, preventedDamage);
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return this.query;
	}

	@Nonnull
	@Override
	public SystemGroup<EntityStore> getGroup() {
		return DamageModule.get().getInspectDamageGroup();
	}

	@Nonnull
	private SkillType resolveRoutedSkill(@Nonnull Damage.Source source, @Nonnull UUID attackerId) {
		if (source instanceof Damage.ProjectileSource) {
			return SkillType.RANGED;
		}

		CombatStyleType style = this.combatStyleService.getCombatStyle(attackerId);
		return style.getGrantedSkill();
	}

	@Nonnull
	private String sourceTag(@Nonnull Damage.Source source, @Nonnull SkillType routedSkill) {
		if (source instanceof Damage.ProjectileSource) {
			return SOURCE_RANGED;
		}

		return SOURCE_MELEE_PREFIX + routedSkill.name().toLowerCase(Locale.ROOT);
	}

	private void grantDefenseBlockXp(int index,
			@Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
			@Nonnull CommandBuffer<EntityStore> commandBuffer,
			boolean blocked,
			double preventedDamage) {
		if (!blocked || preventedDamage <= 0.0D) {
			return;
		}

		Ref<EntityStore> defenderRef = archetypeChunk.getReferenceTo(index);
		PlayerRef defenderPlayerRef = commandBuffer.getComponent(defenderRef, PlayerRef.getComponentType());
		if (defenderPlayerRef == null) {
			return;
		}

		this.skillXpDispatchService.grantSkillXp(
				commandBuffer,
				defenderRef,
				SkillType.DEFENSE,
				preventedDamage * XP_PER_DAMAGE,
				SOURCE_BLOCK_DEFENSE,
				true);
	}
}
