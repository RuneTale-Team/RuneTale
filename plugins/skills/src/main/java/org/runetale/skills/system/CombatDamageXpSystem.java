package org.runetale.skills.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.AllLegacyLivingEntityTypesQuery;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.domain.CombatStyleType;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.progression.service.SkillXpDispatchService;
import org.runetale.skills.service.CombatStyleService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
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
	private static final Field PROJECTILE_CREATOR_UUID_FIELD = resolveProjectileCreatorUuidField();

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

		PlayerRef attackerPlayerRef = resolveAttackerPlayerRef(commandBuffer, source, attackerRef);
		if (attackerPlayerRef == null) {
			grantDefenseBlockXp(index, archetypeChunk, commandBuffer, blocked, preventedDamage);
			return;
		}

		Ref<EntityStore> attackerXpRef = attackerPlayerRef.getReference();
		if (!attackerXpRef.isValid()) {
			attackerXpRef = attackerRef;
		}

		if (finalDamage > 0.0D) {
			grantAttackerDamageXp(commandBuffer, attackerXpRef, attackerPlayerRef.getUuid(), event, source, finalDamage);
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

	private void grantAttackerDamageXp(
			@Nonnull CommandBuffer<EntityStore> commandBuffer,
			@Nonnull Ref<EntityStore> attackerRef,
			@Nonnull UUID attackerId,
			@Nonnull Damage event,
			@Nonnull Damage.Source source,
			double finalDamage) {
		long totalXp = Math.max(0L, Math.round(finalDamage * XP_PER_DAMAGE));
		if (totalXp <= 0L) {
			return;
		}

		if (isRangedDamage(event, source)) {
			grantXp(commandBuffer, attackerRef, SkillType.RANGED, totalXp, SOURCE_RANGED);
			return;
		}

		CombatStyleType style = this.combatStyleService.getCombatStyle(attackerId);
		if (style.isControlledSplit()) {
			long attackXp = totalXp / 3L;
			long strengthXp = totalXp / 3L;
			long defenseXp = totalXp / 3L;

			long remainder = totalXp - (attackXp + strengthXp + defenseXp);
			if (remainder >= 1L) {
				attackXp++;
			}
			if (remainder >= 2L) {
				strengthXp++;
			}

			grantXp(commandBuffer, attackerRef, SkillType.ATTACK, attackXp, SOURCE_MELEE_PREFIX + "controlled:attack");
			grantXp(commandBuffer, attackerRef, SkillType.STRENGTH, strengthXp, SOURCE_MELEE_PREFIX + "controlled:strength");
			grantXp(commandBuffer, attackerRef, SkillType.DEFENSE, defenseXp, SOURCE_MELEE_PREFIX + "controlled:defense");
			return;
		}

		if (style == CombatStyleType.ACCURATE) {
			grantXp(commandBuffer, attackerRef, SkillType.ATTACK, totalXp, SOURCE_MELEE_PREFIX + "accurate");
			return;
		}

		if (style == CombatStyleType.AGGRESSIVE) {
			grantXp(commandBuffer, attackerRef, SkillType.STRENGTH, totalXp, SOURCE_MELEE_PREFIX + "aggressive");
			return;
		}

		grantXp(commandBuffer, attackerRef, SkillType.DEFENSE, totalXp, SOURCE_MELEE_PREFIX + "defensive");
	}

	private void grantXp(
			@Nonnull CommandBuffer<EntityStore> commandBuffer,
			@Nonnull Ref<EntityStore> playerRef,
			@Nonnull SkillType skillType,
			long amount,
			@Nonnull String sourceTag) {
		if (amount <= 0L) {
			return;
		}

		this.skillXpDispatchService.grantSkillXp(
				commandBuffer,
				playerRef,
				skillType,
				amount,
				sourceTag.toLowerCase(Locale.ROOT),
				true);
	}

	private boolean isRangedDamage(@Nonnull Damage event, @Nonnull Damage.Source source) {
		if (source instanceof Damage.ProjectileSource) {
			return true;
		}

		DamageCause cause = event.getCause();
		if (cause == null) {
			return false;
		}

		if (cause == DamageCause.PROJECTILE) {
			return true;
		}

		String causeId = cause.getId();
		return causeId != null && causeId.toLowerCase(Locale.ROOT).contains("projectile");
	}

	@Nullable
	private PlayerRef resolveAttackerPlayerRef(
			@Nonnull CommandBuffer<EntityStore> commandBuffer,
			@Nonnull Damage.Source source,
			@Nonnull Ref<EntityStore> sourceRef) {
		PlayerRef direct = commandBuffer.getComponent(sourceRef, PlayerRef.getComponentType());
		if (direct != null) {
			return direct;
		}

		if (!(source instanceof Damage.ProjectileSource projectileSource)) {
			return null;
		}

		Ref<EntityStore> projectileRef = projectileSource.getProjectile();
		ProjectileComponent projectileComponent = commandBuffer.getComponent(projectileRef, ProjectileComponent.getComponentType());
		UUID creatorUuid = extractProjectileCreatorUuid(projectileComponent);
		if (creatorUuid == null) {
			return null;
		}

		PlayerRef playerRef = Universe.get().getPlayer(creatorUuid);
		if (playerRef != null && playerRef.isValid()) {
			return playerRef;
		}

		EntityStore entityStore = (EntityStore) commandBuffer.getExternalData();
		Ref<EntityStore> creatorRef = entityStore.getRefFromUUID(creatorUuid);
		if (!creatorRef.isValid()) {
			return null;
		}

		return commandBuffer.getComponent(creatorRef, PlayerRef.getComponentType());
	}

	@Nullable
	private UUID extractProjectileCreatorUuid(@Nullable ProjectileComponent projectileComponent) {
		if (projectileComponent == null || PROJECTILE_CREATOR_UUID_FIELD == null) {
			return null;
		}

		try {
			return (UUID) PROJECTILE_CREATOR_UUID_FIELD.get(projectileComponent);
		} catch (IllegalAccessException ignored) {
			return null;
		}
	}

	@Nullable
	private static Field resolveProjectileCreatorUuidField() {
		try {
			Field field = ProjectileComponent.class.getDeclaredField("creatorUuid");
			field.setAccessible(true);
			return field;
		} catch (Exception ignored) {
			return null;
		}
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
