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
import org.runetale.skills.domain.CombatStyleType;
import org.runetale.skills.service.CombatStyleService;

import javax.annotation.Nonnull;

/**
 * Lightweight custom page for selecting player melee XP mode.
 */
public class CombatStylePage extends InteractiveCustomUIPage<CombatStylePage.CombatStylePageEventData> {

	private static final String UI_PATH = "SkillsPlugin/CombatStylePicker.ui";
	private static final String SELECTED_SUFFIX = " [Selected]";

	private final CombatStyleService combatStyleService;

	public CombatStylePage(
			@Nonnull PlayerRef playerRef,
			@Nonnull CombatStyleService combatStyleService) {
		super(playerRef, CustomPageLifetime.CanDismiss, CombatStylePageEventData.CODEC);
		this.combatStyleService = combatStyleService;
	}

	@Override
	public void build(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull UIEventBuilder eventBuilder,
			@Nonnull Store<EntityStore> store) {
		commandBuilder.append(UI_PATH);
		bindEvents(eventBuilder);
		render(commandBuilder);
	}

	@Override
	public void handleDataEvent(
			@Nonnull Ref<EntityStore> ref,
			@Nonnull Store<EntityStore> store,
			@Nonnull CombatStylePageEventData data) {
		if (data.action == null || data.action.isBlank()) {
			return;
		}

		if ("Close".equalsIgnoreCase(data.action)) {
			this.close();
			return;
		}

		CombatStyleType selected = CombatStyleType.tryParse(data.action);
		if (selected == null) {
			return;
		}

		this.combatStyleService.setCombatStyle(this.playerRef.getUuid(), selected);

		UICommandBuilder commandBuilder = new UICommandBuilder();
		render(commandBuilder);
		this.sendUpdate(commandBuilder, false);
	}

	private void bindEvents(@Nonnull UIEventBuilder eventBuilder) {
		eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ModeAccurate", EventData.of("Action", CombatStyleType.ACCURATE.getId()), false);
		eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ModeAggressive", EventData.of("Action", CombatStyleType.AGGRESSIVE.getId()), false);
		eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ModeDefensive", EventData.of("Action", CombatStyleType.DEFENSIVE.getId()), false);
		eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ModeControlled", EventData.of("Action", CombatStyleType.CONTROLLED.getId()), false);
		eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "Close"), false);
	}

	private void render(@Nonnull UICommandBuilder commandBuilder) {
		CombatStyleType current = this.combatStyleService.getCombatStyle(this.playerRef.getUuid());

		commandBuilder.set("#CurrentModeValue.Text", current.getDisplayName());
		commandBuilder.set("#CurrentModeDesc.Text", current.describeMeleeXpRouting());

		configureButton(commandBuilder, CombatStyleType.ACCURATE, "#ModeAccurate", "#ModeAccurateDesc", current);
		configureButton(commandBuilder, CombatStyleType.AGGRESSIVE, "#ModeAggressive", "#ModeAggressiveDesc", current);
		configureButton(commandBuilder, CombatStyleType.DEFENSIVE, "#ModeDefensive", "#ModeDefensiveDesc", current);
		configureButton(commandBuilder, CombatStyleType.CONTROLLED, "#ModeControlled", "#ModeControlledDesc", current);
	}

	private void configureButton(
			@Nonnull UICommandBuilder commandBuilder,
			@Nonnull CombatStyleType mode,
			@Nonnull String buttonSelector,
			@Nonnull String descSelector,
			@Nonnull CombatStyleType current) {
		boolean selected = mode == current;
		String label = mode.getDisplayName() + (selected ? SELECTED_SUFFIX : "");
		commandBuilder.set(buttonSelector + ".Text", label);
		commandBuilder.set(descSelector + ".Text", mode.describeMeleeXpRouting());
	}

	public static class CombatStylePageEventData {
		private static final String KEY_ACTION = "Action";

		public static final BuilderCodec<CombatStylePageEventData> CODEC = BuilderCodec
				.builder(CombatStylePageEventData.class, CombatStylePageEventData::new)
				.append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (entry, value) -> entry.action = value, entry -> entry.action)
				.add()
				.build();

		private String action;
	}
}
