package org.runetale.skills.page;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.service.SkillNodeLookupService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public class SkillsOverviewPage extends InteractiveCustomUIPage<SkillsOverviewPage.SkillsPageEventData> {

	private static final int MAX_ROADMAP_CARDS = 6;
	private static final String SKILL_SUBMENU_CARD_TEMPLATE = "SkillsPlugin/SkillSubmenuCard.ui";
	private static final String SKILL_LIST_ITEM_TEMPLATE = "SkillsPlugin/SkillListItem.ui";
	private static final String CARD_ROW_INLINE = "Group { LayoutMode: Left; Anchor: (Bottom: 10); }";
	private static final String CARD_COLUMN_SPACER_INLINE = "Group { Anchor: (Width: 10); }";

	private final SkillsRuntimeApi runtimeApi;
	private final SkillNodeLookupService nodeLookupService;

	@Nullable
	private SkillType selectedSkill;

	public SkillsOverviewPage(
			@Nonnull PlayerRef playerRef,
			@Nonnull SkillsRuntimeApi runtimeApi,
			@Nonnull SkillNodeLookupService nodeLookupService) {
		super(playerRef, CustomPageLifetime.CanDismiss, SkillsPageEventData.CODEC);
		this.runtimeApi = runtimeApi;
		this.nodeLookupService = nodeLookupService;
	}

	@Override
	public void build(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder,
			@Nonnull Store<EntityStore> store) {
		this.selectedSkill = null;
		commandBuilder.append("SkillsPlugin/SkillsOverview.ui");
		eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton", EventData.of("Action", "Back"), false);
		this.render(ref, store, commandBuilder, eventBuilder);
	}

	@Override
	public void handleDataEvent(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull SkillsPageEventData data) {
		SkillType previousSelectedSkill = this.selectedSkill;

		if ("Back".equalsIgnoreCase(data.action)) {
			if (this.selectedSkill != null) {
				this.selectedSkill = null;
			}
		}

		if (data.skill != null) {
			SkillType parsed = parseSkill(data.skill);
			if (parsed != null) {
				if (this.selectedSkill != parsed) {
					this.selectedSkill = parsed;
				}
			}
		}

		if (data.index != null) {
			try {
				int index = Integer.parseInt(data.index);
				if (index >= 0 && index < SkillType.values().length) {
					SkillType indexedSkill = SkillType.values()[index];
					if (this.selectedSkill != indexedSkill) {
						this.selectedSkill = indexedSkill;
					}
				}
			} catch (NumberFormatException ignored) {
			}
		}

		if (previousSelectedSkill == this.selectedSkill) {
			return;
		}

		UICommandBuilder commandBuilder = new UICommandBuilder();
		UIEventBuilder eventBuilder = new UIEventBuilder();
		this.render(ref, store, commandBuilder, eventBuilder);
		this.sendUpdate(commandBuilder, eventBuilder, false);
	}

	private void render(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder) {
		configureStaticLayout(commandBuilder);
		buildSkillList(ref, store, commandBuilder, eventBuilder);
		if (this.selectedSkill == null) {
			renderOverview(ref, store, commandBuilder, eventBuilder);
		} else {
			renderDetail(ref, store, commandBuilder, eventBuilder, this.selectedSkill);
		}
	}

	private void configureStaticLayout(@Nonnull UICommandBuilder commandBuilder) {
		commandBuilder.set("#SearchInput.Visible", false);
		commandBuilder.set("#SendToChatButton.Visible", false);
		commandBuilder.set("#AliasesSection.Visible", false);
		commandBuilder.set("#PermissionSection.Visible", false);
		commandBuilder.set("#VariantsSection.Visible", false);
	}

	private void buildSkillList(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder) {
		commandBuilder.clear("#CommandList");
		for (int i = 0; i < SkillType.values().length; i++) {
			SkillType skill = SkillType.values()[i];
			int level = this.runtimeApi.getSkillLevel(store, ref, skill);
			boolean selected = this.selectedSkill == skill;
			String selector = "#CommandList[" + i + "]";
			commandBuilder.append("#CommandList", SKILL_LIST_ITEM_TEMPLATE);
			commandBuilder.set(selector + " #SkillLabel.Text", formatSkillName(skill) + "  Lv " + level);
			commandBuilder.set(selector + " #SkillIcon.Background", skillIconTexturePath(skill));
			commandBuilder.set(selector + " #SelectedAccent.Visible", selected);
			eventBuilder.addEventBinding(
					CustomUIEventBindingType.Activating,
					selector,
					EventData.of("Index", Integer.toString(i)),
					false);
		}
	}

	private void renderOverview(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder) {
		long totalXp = 0L;
		int totalLevel = 0;

		for (SkillType skill : SkillType.values()) {
			int level = this.runtimeApi.getSkillLevel(store, ref, skill);
			long xp = this.runtimeApi.getSkillExperience(store, ref, skill);
			totalXp += xp;
			totalLevel += level;
		}

		commandBuilder.set("#BackButton.Visible", false);
		commandBuilder.set("#CommandName.Text", "Skills Overview");
		commandBuilder.set("#CommandDescription.Text", "Total Level: " + totalLevel);
		commandBuilder.set("#CommandUsageLabel.Text", "Total XP: " + formatNumber(totalXp));
		commandBuilder.set("#SkillsSectionTitle.Text", "Skills");

		commandBuilder.set("#SubcommandSection.Visible", true);
		commandBuilder.clear("#SubcommandCards");

		int cardIndex = 0;
		int maxLevel = this.runtimeApi.getMaxLevel();
		for (SkillType skill : SkillType.values()) {
			int skillIndex = skill.ordinal();
			int level = this.runtimeApi.getSkillLevel(store, ref, skill);
			long xp = this.runtimeApi.getSkillExperience(store, ref, skill);
			long current = xpProgressCurrent(level, xp);
			long required = xpProgressRequired(level);
			String usage = level >= maxLevel ? "Lv " + maxLevel + " (MAX)" : "Lv " + level + "  Progress " + current + "/" + required;
			appendCard(commandBuilder, eventBuilder, cardIndex++, formatSkillName(skill), usage, formatNumber(xp) + " XP total", skillIndex, skill);
		}
	}

	private void renderDetail(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder,
			@Nonnull SkillType skill) {
		int level = this.runtimeApi.getSkillLevel(store, ref, skill);
		long xp = this.runtimeApi.getSkillExperience(store, ref, skill);
		long current = xpProgressCurrent(level, xp);
		long required = xpProgressRequired(level);
		long nextLevelGap = xpToNextLevel(level, xp);
		int maxLevel = this.runtimeApi.getMaxLevel();

		commandBuilder.set("#BackButton.Visible", true);
		commandBuilder.set("#CommandName.Text", formatSkillName(skill) + " Details");
		commandBuilder.set("#SkillsSectionTitle.Text", "Roadmap");
		if (level >= maxLevel) {
			commandBuilder.set("#CommandDescription.Text", "Level " + maxLevel + " reached. Progression is capped.");
			commandBuilder.set("#CommandUsageLabel.Text", "Total XP: " + formatNumber(xp));
		} else {
			commandBuilder.set("#CommandDescription.Text", "Level " + level + "  |  Progress " + current + "/" + required + " (current/required)");
			commandBuilder.set("#CommandUsageLabel.Text", "XP to next level: " + formatNumber(nextLevelGap));
		}

		commandBuilder.set("#SubcommandSection.Visible", true);
		commandBuilder.clear("#SubcommandCards");

		int cardIndex = 0;
		appendCard(commandBuilder, eventBuilder, cardIndex++, "Current", "Lv " + level, formatNumber(xp) + " XP total", null, null);
		if (level >= maxLevel) {
			appendCard(commandBuilder, eventBuilder, cardIndex++, "Next Milestone", "MAX", "No further level requirement", null, null);
		} else {
			appendCard(commandBuilder, eventBuilder, cardIndex++, "Next Milestone", "Lv " + (level + 1), formatNumber(nextLevelGap) + " XP remaining", null, null);
		}

		List<SkillNodeDefinition> nodes = this.nodeLookupService.listDefinitionsForSkill(skill);
		if (nodes.isEmpty() && isCombatRoadmapSkill(skill)) {
			appendCombatRoadmap(commandBuilder, eventBuilder, cardIndex, skill, level);
			return;
		}

		int shown = 0;
		int unlockedShown = 0;
		int upcomingShown = 0;
		for (SkillNodeDefinition node : nodes) {
			if (shown >= MAX_ROADMAP_CARDS) {
				break;
			}
			boolean unlocked = level >= node.getRequiredSkillLevel();
			if (unlocked && unlockedShown >= 3) {
				continue;
			}
			if (!unlocked && upcomingShown >= 3) {
				continue;
			}
			String state = unlocked ? "Unlocked" : "Locked";
			String usage = "Current/Required Lv " + level + "/" + node.getRequiredSkillLevel();
			String description = state + "  |  +" + Math.round(node.getExperienceReward()) + " XP";
			appendCard(commandBuilder, eventBuilder, cardIndex++, displayNodeName(node), usage, description, null, null);
			if (unlocked) {
				unlockedShown++;
			} else {
				upcomingShown++;
			}
			shown++;
		}

		if (nodes.size() > shown) {
			appendCard(commandBuilder, eventBuilder, cardIndex, "More Nodes", "+" + (nodes.size() - shown), "Additional roadmap entries available", null, null);
		}
	}

	private void appendCard(
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder,
			int cardIndex,
			@Nonnull String title,
			@Nonnull String usage,
			@Nonnull String description,
			@Nullable Integer clickSkillIndex,
			@Nullable SkillType iconSkill) {
		appendCardInternal(commandBuilder, eventBuilder, cardIndex, title, usage, description, clickSkillIndex, null,
				iconSkill);
	}

	private void appendCardInternal(
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder,
			int cardIndex,
			@Nonnull String title,
			@Nonnull String usage,
			@Nonnull String description,
			@Nullable Integer clickSkillIndex,
			@Nullable String action,
			@Nullable SkillType iconSkill) {
		int row = cardIndex / 2;
		int col = cardIndex % 2;
		if (col == 0) {
			commandBuilder.appendInline("#SubcommandCards", CARD_ROW_INLINE);
		} else {
			commandBuilder.appendInline("#SubcommandCards[" + row + "]", CARD_COLUMN_SPACER_INLINE);
		}

		// Row layout: [0]=left card, [1]=spacer, [2]=right card.
		int uiCol = col == 0 ? 0 : 2;
		String cardSelector = "#SubcommandCards[" + row + "][" + uiCol + "]";
		commandBuilder.append("#SubcommandCards[" + row + "]", SKILL_SUBMENU_CARD_TEMPLATE);
		commandBuilder.set(cardSelector + " #IconContainer.Visible", iconSkill != null);
		if (iconSkill != null) {
			commandBuilder.set(cardSelector + " #SubcommandIcon.Background", skillIconTexturePath(iconSkill));
		}
		commandBuilder.set(cardSelector + " #SubcommandName.Text", title);
		commandBuilder.set(cardSelector + " #SubcommandUsage.Text", usage);
		commandBuilder.set(cardSelector + " #SubcommandDescription.Text", description);

		if (clickSkillIndex != null) {
			eventBuilder.addEventBinding(
					CustomUIEventBindingType.Activating,
					cardSelector,
					EventData.of("Index", Integer.toString(clickSkillIndex)),
					false);
		}

		if (action != null && !action.isBlank()) {
			eventBuilder.addEventBinding(
					CustomUIEventBindingType.Activating,
					cardSelector,
					EventData.of("Action", action),
					false);
		}
	}

	@Nullable
	private SkillType parseSkill(@Nullable String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return SkillType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	@Nonnull
	private String formatSkillName(@Nonnull SkillType skillType) {
		String lowered = skillType.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lowered.charAt(0)) + lowered.substring(1);
	}

	@Nonnull
	private String displayNodeName(@Nonnull SkillNodeDefinition node) {
		String label = node.getLabel();
		if (label != null && !label.isBlank()) {
			return label;
		}
		String id = node.getId();
		if (id == null || id.isBlank()) {
			return "unknown";
		}
		return id;
	}

	@Nonnull
	private String formatNumber(long value) {
		return String.format(Locale.ROOT, "%,d", value);
	}

	@Nonnull
	private String skillIconTexturePath(@Nullable SkillType skill) {
		String id = skill == null ? "unknown" : skill.name().toLowerCase(Locale.ROOT);
		return "SkillsPlugin/Assets/Icons/icon_" + id + ".png";
	}

	private boolean isCombatRoadmapSkill(@Nonnull SkillType skill) {
		return skill == SkillType.ATTACK || skill == SkillType.RANGED || skill == SkillType.DEFENSE;
	}

	private void appendCombatRoadmap(
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder,
			int cardIndex,
			@Nonnull SkillType skill,
			int level) {
		if (skill == SkillType.ATTACK) {
			appendCard(commandBuilder, eventBuilder, cardIndex++, "Training Focus", "Melee precision", "Accurate style routes XP into Attack", null, skill);
			appendCard(commandBuilder, eventBuilder, cardIndex++, "Roadmap", "Weapon upgrades", "Higher-tier weapons scale Attack training speed", null, skill);
			appendCard(commandBuilder, eventBuilder, cardIndex, "Roadmap", "Boss unlocks", "Future content ties Attack levels to PvE milestones", null, skill);
			return;
		}

		if (skill == SkillType.RANGED) {
			appendCard(commandBuilder, eventBuilder, cardIndex++, "Training Focus", "Distance combat", "Ranged damage grants direct Ranged XP", null, skill);
			appendCard(commandBuilder, eventBuilder, cardIndex++, "Roadmap", "Ammo progression", "Future arrows and bows unlock through level bands", null, skill);
			appendCard(commandBuilder, eventBuilder, cardIndex, "Roadmap", "Encounters", "Planned encounters will reward ranged specialization", null, skill);
			return;
		}

		String milestone = level < 40 ? "Defensive basics" : "Tank progression";
		appendCard(commandBuilder, eventBuilder, cardIndex++, "Training Focus", milestone, "Blocking damage grants Defense XP", null, skill);
		appendCard(commandBuilder, eventBuilder, cardIndex++, "Roadmap", "Armor scaling", "Higher defense levels support stronger gear sets", null, skill);
		appendCard(commandBuilder, eventBuilder, cardIndex, "Roadmap", "Encounter roles", "Future PvE content will include dedicated tank checks", null, skill);
	}

	private long xpProgressCurrent(int level, long totalXp) {
		int maxLevel = this.runtimeApi.getMaxLevel();
		int safeLevel = Math.max(1, Math.min(maxLevel, level));
		if (safeLevel >= maxLevel) {
			return 0L;
		}
		long levelStartXp = this.runtimeApi.xpForLevel(safeLevel);
		long required = xpProgressRequired(safeLevel);
		long current = Math.max(0L, totalXp - levelStartXp);
		return Math.min(current, required);
	}

	private long xpProgressRequired(int level) {
		int maxLevel = this.runtimeApi.getMaxLevel();
		int safeLevel = Math.max(1, Math.min(maxLevel, level));
		if (safeLevel >= maxLevel) {
			return 0L;
		}
		long start = this.runtimeApi.xpForLevel(safeLevel);
		long next = this.runtimeApi.xpForLevel(safeLevel + 1);
		return Math.max(1L, next - start);
	}

	private long xpToNextLevel(int level, long totalXp) {
		if (level >= this.runtimeApi.getMaxLevel()) {
			return 0L;
		}
		long required = xpProgressRequired(level);
		long current = xpProgressCurrent(level, totalXp);
		return Math.max(0L, required - current);
	}

	public static class SkillsPageEventData {
		private static final String KEY_ACTION = "Action";
		private static final String KEY_SKILL = "Skill";
		private static final String KEY_INDEX = "Index";

		public static final BuilderCodec<SkillsPageEventData> CODEC = BuilderCodec
				.builder(SkillsPageEventData.class, SkillsPageEventData::new)
				.append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (entry, value) -> entry.action = value, entry -> entry.action)
				.add()
				.append(new KeyedCodec<>(KEY_SKILL, Codec.STRING), (entry, value) -> entry.skill = value, entry -> entry.skill)
				.add()
				.append(new KeyedCodec<>(KEY_INDEX, Codec.STRING), (entry, value) -> entry.index = value, entry -> entry.index)
				.add()
				.build();

		private String action;
		private String skill;
		private String index;
	}
}
