package org.runetale.testing.junit;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.BeforeEachCallback;

/**
 * Configures the JVM log manager property required by {@code HytaleLogger}.
 */
public class HytaleLoggerExtension implements BeforeEachCallback, AfterEachCallback {

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
			.create(HytaleLoggerExtension.class);
	private static final String PREVIOUS_LOG_MANAGER_KEY = "previous-log-manager";
	private static final String LOG_MANAGER_PROPERTY = "java.util.logging.manager";
	private static final String HYTALE_LOG_MANAGER = "com.hypixel.hytale.logger.backend.HytaleLogManager";

	@Override
	public void beforeEach(ExtensionContext context) {
		ExtensionContext.Store store = context.getStore(NAMESPACE);
		store.put(PREVIOUS_LOG_MANAGER_KEY, System.getProperty(LOG_MANAGER_PROPERTY));
		System.setProperty(LOG_MANAGER_PROPERTY, HYTALE_LOG_MANAGER);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		ExtensionContext.Store store = context.getStore(NAMESPACE);
		String previousValue = store.get(PREVIOUS_LOG_MANAGER_KEY, String.class);
		if (previousValue == null || previousValue.isBlank()) {
			System.clearProperty(LOG_MANAGER_PROPERTY);
		} else {
			System.setProperty(LOG_MANAGER_PROPERTY, previousValue);
		}
	}
}
