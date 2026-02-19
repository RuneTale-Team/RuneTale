package org.runetale.skills.component;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.domain.SkillProgress;
import org.runetale.skills.domain.SkillType;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistent per-player skill progression component.
 *
 * <p>
 * The serialized shape uses a string-keyed map so future skills can be added
 * without requiring
 * schema migration. Unknown keys remain harmless and can be ignored by runtime
 * logic.
 */
public class PlayerSkillProfileComponent implements Component<EntityStore> {

	/**
	 * Builder codec used to serialize/deserialize profile data through ECS
	 * persistence.
	 */
	public static final BuilderCodec<PlayerSkillProfileComponent> CODEC = BuilderCodec.builder(
			PlayerSkillProfileComponent.class,
			PlayerSkillProfileComponent::new)
			.append(
					new KeyedCodec<>("SkillProgress", new MapCodec<>(SkillProgress.CODEC, HashMap::new, false)),
					(component, map) -> component.skillProgressByName = map == null ? new HashMap<>() : map,
					PlayerSkillProfileComponent::getRawSkillProgressByName)
			.add()
			.build();

	/**
	 * Underlying persisted map: key is enum name (e.g. WOODCUTTING), value is
	 * progression state.
	 */
	private Map<String, SkillProgress> skillProgressByName;

	/**
	 * Codec constructor.
	 */
	protected PlayerSkillProfileComponent() {
		this.skillProgressByName = new HashMap<>();
	}

	/**
	 * Returns mutable progression for a skill, creating a default entry when
	 * missing.
	 */
	@Nonnull
	public SkillProgress getOrCreate(@Nonnull SkillType skillType) {
		return skillProgressByName.computeIfAbsent(skillType.name(), ignored -> new SkillProgress(0L, 1));
	}

	/**
	 * Returns XP for the given skill, defaulting to 0 when absent.
	 */
	public long getExperience(@Nonnull SkillType skillType) {
		return getOrCreate(skillType).getExperience();
	}

	/**
	 * Returns level for the given skill, defaulting to level 1 when absent.
	 */
	public int getLevel(@Nonnull SkillType skillType) {
		return getOrCreate(skillType).getLevel();
	}

	/**
	 * Mutates skill progression in-place.
	 */
	public void set(@Nonnull SkillType skillType, long experience, int level) {
		getOrCreate(skillType).set(experience, level);
	}

	/**
	 * Exposes raw persisted map to the codec.
	 */
	@Nonnull
	public Map<String, SkillProgress> getRawSkillProgressByName() {
		return skillProgressByName;
	}

	/**
	 * Deep clone for ECS structural operations.
	 */
	@Nonnull
	@Override
	public Component<EntityStore> clone() {
		PlayerSkillProfileComponent copy = new PlayerSkillProfileComponent();
		for (Map.Entry<String, SkillProgress> entry : this.skillProgressByName.entrySet()) {
			SkillProgress progress = entry.getValue();
			copy.skillProgressByName.put(entry.getKey(),
					new SkillProgress(progress.getExperience(), progress.getLevel()));
		}
		return copy;
	}
}
