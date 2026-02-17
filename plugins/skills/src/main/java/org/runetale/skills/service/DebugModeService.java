package org.runetale.skills.service;

import javax.annotation.Nonnull;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains runtime debug-mode toggles per plugin key.
 */
public class DebugModeService {

	private final Set<String> supportedPlugins;
	private final Map<String, Boolean> enabledByPlugin;

	public DebugModeService(@Nonnull List<String> supportedPlugins) {
		LinkedHashSet<String> normalized = new LinkedHashSet<>();
		for (String plugin : supportedPlugins) {
			String normalizedPlugin = normalize(plugin);
			if (!normalizedPlugin.isEmpty()) {
				normalized.add(normalizedPlugin);
			}
		}

		this.supportedPlugins = Set.copyOf(normalized);
		this.enabledByPlugin = new ConcurrentHashMap<>();
		for (String plugin : this.supportedPlugins) {
			this.enabledByPlugin.put(plugin, false);
		}
	}

	public boolean isSupported(@Nonnull String plugin) {
		return this.supportedPlugins.contains(normalize(plugin));
	}

	public boolean isEnabled(@Nonnull String plugin) {
		return this.enabledByPlugin.getOrDefault(normalize(plugin), false);
	}

	public boolean enable(@Nonnull String plugin) {
		String normalized = normalize(plugin);
		if (!this.supportedPlugins.contains(normalized)) {
			return false;
		}
		this.enabledByPlugin.put(normalized, true);
		return true;
	}

	public boolean disable(@Nonnull String plugin) {
		String normalized = normalize(plugin);
		if (!this.supportedPlugins.contains(normalized)) {
			return false;
		}
		this.enabledByPlugin.put(normalized, false);
		return true;
	}

	@Nonnull
	public List<String> supportedPlugins() {
		return List.copyOf(this.supportedPlugins);
	}

	@Nonnull
	private static String normalize(@Nonnull String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}
}
