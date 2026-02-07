package org.runetale.skills.page;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.asset.SkillNodeDefinition;
import org.runetale.skills.component.PlayerSkillProfileComponent;
import org.runetale.skills.domain.SkillType;
import org.runetale.skills.service.OsrsXpService;
import org.runetale.skills.service.SkillNodeLookupService;
import org.runetale.skills.service.SkillSessionStatsService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

public class SkillsOverviewPage extends InteractiveCustomUIPage<SkillsOverviewPage.SkillsPageEventData> {

	private static final int MAX_LEVEL = 99;
	private static final int MAX_ROADMAP_CARDS = 6;
	private static final String SKILL_CARD_TEMPLATE = "SkillsPlugin/SkillCard.ui";
	private static final Value<String> BASIC_BUTTON_STYLE = Value.ref("Pages/BasicTextButton.ui", "LabelStyle");
	private static final Value<String> BASIC_BUTTON_STYLE_SELECTED = Value.ref("Pages/BasicTextButton.ui", "SelectedLabelStyle");

	private final ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType;
	private final OsrsXpService xpService;
	private final SkillNodeLookupService nodeLookupService;
	private final SkillSessionStatsService sessionStatsService;

	@Nullable
	private SkillType selectedSkill;

	public SkillsOverviewPage(
			@Nonnull PlayerRef playerRef,
			@Nonnull ComponentType<EntityStore, PlayerSkillProfileComponent> profileComponentType,
			@Nonnull OsrsXpService xpService,
			@Nonnull SkillNodeLookupService nodeLookupService,
			@Nonnull SkillSessionStatsService sessionStatsService) {
		super(playerRef, CustomPageLifetime.CanDismiss, SkillsPageEventData.CODEC);
		this.profileComponentType = profileComponentType;
		this.xpService = xpService;
		this.nodeLookupService = nodeLookupService;
		this.sessionStatsService = sessionStatsService;
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
		if ("Back".equalsIgnoreCase(data.action)) {
			this.selectedSkill = null;
		}

		if ("ToggleTrack".equalsIgnoreCase(data.action) && this.selectedSkill != null) {
			SkillType tracked = this.sessionStatsService.getTrackedSkill(this.playerRef.getUuid());
			if (tracked == this.selectedSkill) {
				this.sessionStatsService.clearTrackedSkill(this.playerRef.getUuid());
			} else {
				this.sessionStatsService.setTrackedSkill(this.playerRef.getUuid(), this.selectedSkill);
			}
		}

		if (data.skill != null) {
			SkillType parsed = parseSkill(data.skill);
			if (parsed != null) {
				this.selectedSkill = parsed;
			}
		}

		if (data.index != null) {
			try {
				int index = Integer.parseInt(data.index);
				if (index >= 0 && index < SkillType.values().length) {
					this.selectedSkill = SkillType.values()[index];
				}
			} catch (NumberFormatException ignored) {
			}
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
		PlayerSkillProfileComponent profile = store.getComponent(ref, this.profileComponentType);
		configureStaticLayout(commandBuilder);
		buildSkillList(profile, commandBuilder, eventBuilder);
		if (this.selectedSkill == null) {
			renderOverview(profile, commandBuilder, eventBuilder);
		} else {
			renderDetail(profile, commandBuilder, eventBuilder, this.selectedSkill);
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
			@Nullable PlayerSkillProfileComponent profile,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder) {
		commandBuilder.clear("#CommandList");
		SkillType trackedSkill = this.sessionStatsService.getTrackedSkill(this.playerRef.getUuid());
		for (int i = 0; i < SkillType.values().length; i++) {
			SkillType skill = SkillType.values()[i];
			int level = profile == null ? 1 : profile.getLevel(skill);
			boolean selected = this.selectedSkill == skill;
			String selector = "#CommandList[" + i + "]";
			commandBuilder.append("#CommandList", "Pages/BasicTextButton.ui");
			commandBuilder.set(selector + ".Text", decorateSkillTitle(skill, trackedSkill, selected) + "  Lv " + level);
			commandBuilder.set(selector + ".Style", selected ? BASIC_BUTTON_STYLE_SELECTED : BASIC_BUTTON_STYLE);
			eventBuilder.addEventBinding(
					CustomUIEventBindingType.Activating,
					selector,
					EventData.of("Index", Integer.toString(i)),
					false);
		}
	}

	private void renderOverview(
			@Nullable PlayerSkillProfileComponent profile,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder) {
		long totalXp = 0L;
		int totalLevel = 0;

		for (SkillType skill : SkillType.values()) {
			int level = profile == null ? 1 : profile.getLevel(skill);
			long xp = profile == null ? 0L : profile.getExperience(skill);
			totalXp += xp;
			totalLevel += level;
		}

		SkillType trackedSkill = this.sessionStatsService.getTrackedSkill(this.playerRef.getUuid());
		String trackedLabel = trackedSkill == null ? "None" : formatSkillName(trackedSkill);

		commandBuilder.set("#BackButton.Visible", false);
		commandBuilder.set("#CommandName.Text", "Skills Overview");
		commandBuilder.set("#CommandDescription.Text", "Total Level: " + totalLevel + " | Tracked: " + trackedLabel);
		commandBuilder.set("#CommandUsageLabel.Text", "Total XP: " + formatNumber(totalXp) + " | Select a skill to inspect details");
		commandBuilder.set("#SkillsSectionTitle.Text", "Skills");

		commandBuilder.set("#SubcommandSection.Visible", true);
		commandBuilder.clear("#SubcommandCards");

		int cardIndex = 0;
		for (SkillType skill : SkillType.values()) {
			int skillIndex = skill.ordinal();
			int level = profile == null ? 1 : profile.getLevel(skill);
			long xp = profile == null ? 0L : profile.getExperience(skill);
			long current = xpProgressCurrent(level, xp);
			long required = xpProgressRequired(level);
			String usage = level >= MAX_LEVEL ? "Lv 99 (MAX)" : "Lv " + level + "  Progress " + current + "/" + required;
			appendCard(commandBuilder, eventBuilder, cardIndex++, formatSkillName(skill), usage, formatNumber(xp) + " XP total", skill, skillIndex);
		}
	}

	private void renderDetail(
			@Nullable PlayerSkillProfileComponent profile,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder,
			@Nonnull SkillType skill) {
		int level = profile == null ? 1 : profile.getLevel(skill);
		long xp = profile == null ? 0L : profile.getExperience(skill);
		long current = xpProgressCurrent(level, xp);
		long required = xpProgressRequired(level);
		long nextLevelGap = xpToNextLevel(level, xp);
		SkillType trackedSkill = this.sessionStatsService.getTrackedSkill(this.playerRef.getUuid());
		boolean isTracked = trackedSkill == skill;

		commandBuilder.set("#BackButton.Visible", true);
		commandBuilder.set("#CommandName.Text", formatSkillName(skill) + " Details");
		commandBuilder.set("#SkillsSectionTitle.Text", "Roadmap");
		if (level >= MAX_LEVEL) {
			commandBuilder.set("#CommandDescription.Text", "Level 99 reached. Progression is capped.");
			commandBuilder.set("#CommandUsageLabel.Text", "Total XP: " + formatNumber(xp));
		} else {
			commandBuilder.set("#CommandDescription.Text", "Level " + level + "  |  Progress " + current + "/" + required + " (current/required)");
			commandBuilder.set("#CommandUsageLabel.Text", "XP to next level: " + formatNumber(nextLevelGap));
		}

		commandBuilder.set("#SubcommandSection.Visible", true);
		commandBuilder.clear("#SubcommandCards");

		int cardIndex = 0;
		appendCard(commandBuilder, eventBuilder, cardIndex++, "Current", "Lv " + level, formatNumber(xp) + " XP total", skill, null);
		if (level >= MAX_LEVEL) {
			appendCard(commandBuilder, eventBuilder, cardIndex++, "Next Milestone", "MAX", "No further level requirement", skill, null);
		} else {
			appendCard(commandBuilder, eventBuilder, cardIndex++, "Next Milestone", "Lv " + (level + 1), formatNumber(nextLevelGap) + " XP remaining", skill, null);
		}
		appendCardWithAction(
				commandBuilder,
				eventBuilder,
				cardIndex++,
				isTracked ? "Tracked Skill" : "Track Skill",
				isTracked ? formatSkillName(skill) : "Set as tracked",
				isTracked ? "Click to clear tracked skill" : "Click to prioritize this skill",
				skill,
				"ToggleTrack");

		List<SkillNodeDefinition> nodes = this.nodeLookupService.listDefinitionsForSkill(skill);
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
			String description = state
					+ "  |  Tool " + formatToolTier(node) + " " + formatToolKeyword(node)
					+ "  |  +" + Math.round(node.getExperienceReward()) + " XP";
			appendCard(commandBuilder, eventBuilder, cardIndex++, prettifyNodeId(node.getId()), usage, description, skill, null);
			if (unlocked) {
				unlockedShown++;
			} else {
				upcomingShown++;
			}
			shown++;
		}

		if (nodes.size() > shown) {
			appendCard(commandBuilder, eventBuilder, cardIndex, "More Nodes", "+" + (nodes.size() - shown), "Additional roadmap entries available", skill, null);
		}
	}

	private void appendCard(
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder,
			int cardIndex,
			@Nonnull String title,
			@Nonnull String usage,
			@Nonnull String description,
			@Nullable SkillType iconSkill,
			@Nullable Integer clickSkillIndex) {
		int row = cardIndex / 2;
		int col = cardIndex % 2;
		if (col == 0) {
			commandBuilder.appendInline("#SubcommandCards", "Group { LayoutMode: Left; Anchor: (Bottom: 10); }");
		}

		String cardSelector = "#SubcommandCards[" + row + "][" + col + "]";
		commandBuilder.append("#SubcommandCards[" + row + "]", SKILL_CARD_TEMPLATE);
		commandBuilder.set(cardSelector + " #SubcommandIcon.Text", skillIconToken(iconSkill));
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
	}

	private void appendCardWithAction(
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder,
			int cardIndex,
			@Nonnull String title,
			@Nonnull String usage,
			@Nonnull String description,
			@Nullable SkillType iconSkill,
			@Nonnull String action) {
		int row = cardIndex / 2;
		int col = cardIndex % 2;
		if (col == 0) {
			commandBuilder.appendInline("#SubcommandCards", "Group { LayoutMode: Left; Anchor: (Bottom: 10); }");
		}

		String cardSelector = "#SubcommandCards[" + row + "][" + col + "]";
		commandBuilder.append("#SubcommandCards[" + row + "]", SKILL_CARD_TEMPLATE);
		commandBuilder.set(cardSelector + " #SubcommandIcon.Text", skillIconToken(iconSkill));
		commandBuilder.set(cardSelector + " #SubcommandName.Text", title);
		commandBuilder.set(cardSelector + " #SubcommandUsage.Text", usage);
		commandBuilder.set(cardSelector + " #SubcommandDescription.Text", description);
		eventBuilder.addEventBinding(
				CustomUIEventBindingType.Activating,
				cardSelector,
				EventData.of("Action", action),
				false);
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
	private String prettifyNodeId(@Nonnull String nodeId) {
		String spaced = nodeId.replace('_', ' ').trim();
		if (spaced.isEmpty()) {
			return "Unknown Node";
		}
		return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
	}

	@Nonnull
	private String formatToolTier(@Nonnull SkillNodeDefinition node) {
		String raw = node.getRequiredToolTier().name().toLowerCase(Locale.ROOT);
		if ("none".equals(raw)) {
			return "Any";
		}
		return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
	}

	@Nonnull
	private String formatToolKeyword(@Nonnull SkillNodeDefinition node) {
		String raw = node.getRequiredToolKeyword().toLowerCase(Locale.ROOT).replace('_', ' ').trim();
		raw = raw.replace("tool ", "");
		if (raw.isEmpty()) {
			return "Tool";
		}
		return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
	}

	@Nonnull
	private String formatNumber(long value) {
		return String.format(Locale.ROOT, "%,d", value);
	}

	@Nonnull
	private String decorateSkillTitle(
			@Nonnull SkillType skill,
			@Nullable SkillType trackedSkill,
			boolean selected) {
		StringBuilder label = new StringBuilder();
		if (selected) {
			label.append("> ");
		}
		label.append(skillIconToken(skill)).append(' ');
		label.append(formatSkillName(skill));
		if (trackedSkill == skill) {
			label.append("  [Tracked]");
		}
		return label.toString();
	}

	@Nonnull
	private String skillIconToken(@Nullable SkillType skill) {
		if (skill == null) {
			return "[**]";
		}
		switch (skill) {
			case WOODCUTTING:
				return "[WC]";
			case MINING:
				return "[MN]";
			case FISHING:
				return "[FS]";
			default:
				return "[??]";
		}
	}

	private long xpProgressCurrent(int level, long totalXp) {
		int safeLevel = Math.max(1, Math.min(MAX_LEVEL, level));
		if (safeLevel >= MAX_LEVEL) {
			return 0L;
		}
		long levelStartXp = this.xpService.xpForLevel(safeLevel);
		long required = xpProgressRequired(safeLevel);
		long current = Math.max(0L, totalXp - levelStartXp);
		return Math.min(current, required);
	}

	private long xpProgressRequired(int level) {
		int safeLevel = Math.max(1, Math.min(MAX_LEVEL, level));
		if (safeLevel >= MAX_LEVEL) {
			return 0L;
		}
		long start = this.xpService.xpForLevel(safeLevel);
		long next = this.xpService.xpForLevel(safeLevel + 1);
		return Math.max(1L, next - start);
	}

	private long xpToNextLevel(int level, long totalXp) {
		if (level >= MAX_LEVEL) {
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
