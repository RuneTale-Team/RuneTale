package org.runetale.skills.service;

import org.junit.jupiter.api.Test;
import org.runetale.skills.domain.CombatStyleType;
import org.runetale.skills.domain.SkillType;
import org.runetale.testing.junit.ContractTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContractTest
class CombatXpAllocationServiceContractTest {

	@Test
	void allocateAttackerXpReturnsEmptyWhenDamageDoesNotProduceXp() {
		CombatXpAllocationService service = new CombatXpAllocationService();

		List<CombatXpAllocationService.CombatXpGrant> grants = service.allocateAttackerXp(false,
				CombatStyleType.ACCURATE,
				0.0D);

		assertThat(grants).isEmpty();
	}

	@Test
	void allocateAttackerXpRoutesRangedDamageToRangedSkill() {
		CombatXpAllocationService service = new CombatXpAllocationService();

		List<CombatXpAllocationService.CombatXpGrant> grants = service.allocateAttackerXp(true,
				CombatStyleType.DEFENSIVE,
				2.5D);

		assertThat(grants).singleElement().satisfies(grant -> {
			assertThat(grant.skillType()).isEqualTo(SkillType.RANGED);
			assertThat(grant.amount()).isEqualTo(10L);
			assertThat(grant.normalizedSourceTag()).isEqualTo("combat:ranged");
		});
	}

	@Test
	void allocateAttackerXpRoutesMeleeStylesToExpectedSingleSkill() {
		CombatXpAllocationService service = new CombatXpAllocationService();

		assertThat(service.allocateAttackerXp(false, CombatStyleType.ACCURATE, 2.0D))
				.singleElement().satisfies(grant -> {
					assertThat(grant.skillType()).isEqualTo(SkillType.ATTACK);
					assertThat(grant.amount()).isEqualTo(8L);
					assertThat(grant.normalizedSourceTag()).isEqualTo("combat:melee:accurate");
				});

		assertThat(service.allocateAttackerXp(false, CombatStyleType.AGGRESSIVE, 2.0D))
				.singleElement().satisfies(grant -> {
					assertThat(grant.skillType()).isEqualTo(SkillType.STRENGTH);
					assertThat(grant.amount()).isEqualTo(8L);
					assertThat(grant.normalizedSourceTag()).isEqualTo("combat:melee:aggressive");
				});

		assertThat(service.allocateAttackerXp(false, CombatStyleType.DEFENSIVE, 2.0D))
				.singleElement().satisfies(grant -> {
					assertThat(grant.skillType()).isEqualTo(SkillType.DEFENSE);
					assertThat(grant.amount()).isEqualTo(8L);
					assertThat(grant.normalizedSourceTag()).isEqualTo("combat:melee:defensive");
				});
	}

	@Test
	void allocateAttackerXpSplitsControlledMeleeDamageAcrossThreeSkills() {
		CombatXpAllocationService service = new CombatXpAllocationService();

		List<CombatXpAllocationService.CombatXpGrant> grants = service.allocateAttackerXp(false,
				CombatStyleType.CONTROLLED,
				2.0D);

		assertThat(grants).hasSize(3);
		assertThat(grants.get(0).skillType()).isEqualTo(SkillType.ATTACK);
		assertThat(grants.get(0).amount()).isEqualTo(3L);
		assertThat(grants.get(1).skillType()).isEqualTo(SkillType.STRENGTH);
		assertThat(grants.get(1).amount()).isEqualTo(3L);
		assertThat(grants.get(2).skillType()).isEqualTo(SkillType.DEFENSE);
		assertThat(grants.get(2).amount()).isEqualTo(2L);
	}
}
