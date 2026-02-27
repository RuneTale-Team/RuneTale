package org.runetale.skills.domain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public final class SkillIconPaths {

	private static final String ICON_PATH_PREFIX = "SkillsPlugin/Assets/Icons/icon_";
	private static final String ICON_PATH_SUFFIX = ".png";

	private SkillIconPaths() {
	}

	@Nonnull
	public static String forSkill(@Nullable SkillType skillType) {
		String id = skillType == null ? "unknown" : skillType.name().toLowerCase(Locale.ROOT);
		return ICON_PATH_PREFIX + id + ICON_PATH_SUFFIX;
	}
}
