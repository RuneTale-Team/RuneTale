package org.runetale.testing.junit;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.BeforeEachCallback;

import java.util.Locale;
import java.util.TimeZone;

/**
 * Sets deterministic locale/timezone defaults for test execution.
 */
public class DeterministicEnvironmentExtension implements BeforeEachCallback, AfterEachCallback {

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
			.create(DeterministicEnvironmentExtension.class);
	private static final String PREVIOUS_LOCALE_KEY = "previous-locale";
	private static final String PREVIOUS_TIMEZONE_KEY = "previous-timezone";

	@Override
	public void beforeEach(ExtensionContext context) {
		ExtensionContext.Store store = context.getStore(NAMESPACE);
		store.put(PREVIOUS_LOCALE_KEY, Locale.getDefault());
		store.put(PREVIOUS_TIMEZONE_KEY, TimeZone.getDefault());

		Locale.setDefault(Locale.ROOT);
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	@Override
	public void afterEach(ExtensionContext context) {
		ExtensionContext.Store store = context.getStore(NAMESPACE);
		Locale previousLocale = store.get(PREVIOUS_LOCALE_KEY, Locale.class);
		TimeZone previousTimezone = store.get(PREVIOUS_TIMEZONE_KEY, TimeZone.class);

		if (previousLocale != null) {
			Locale.setDefault(previousLocale);
		}

		if (previousTimezone != null) {
			TimeZone.setDefault(previousTimezone);
		}
	}
}
