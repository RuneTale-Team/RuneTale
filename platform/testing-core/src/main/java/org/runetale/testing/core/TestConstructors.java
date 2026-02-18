package org.runetale.testing.core;

import java.lang.reflect.Constructor;

/**
 * Reflection helpers for constructing test-only fixture objects.
 */
public final class TestConstructors {

	private TestConstructors() {
	}

	public static <T> T instantiateNoArgs(Class<T> type) {
		try {
			Constructor<T> constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Unable to create test instance for " + type.getName(), e);
		}
	}
}
