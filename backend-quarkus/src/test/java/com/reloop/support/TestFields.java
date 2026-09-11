package com.reloop.support;

import java.lang.reflect.Field;

/** Replacement for Spring's ReflectionTestUtils used by the legacy tests. */
public final class TestFields {

    private TestFields() {}

    public static void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field " + fieldName, e);
        }
    }
}
