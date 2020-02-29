package com.github.snorochevskiy.pojoeval.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectionUtils {
    private ReflectionUtils() {

    }

    public static boolean hasField(Class cls, String fieldName) {
        String requiredGetter = getter(fieldName);
        return Arrays.stream(cls.getMethods())
            .anyMatch(m -> requiredGetter.equals(m.getName()) && m.getParameterCount() == 0);
    }

    public static <T> String getFieldValueOrNull(T t, String fieldName) {
        try {
            return getFieldValue(t, fieldName);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    public static <T> String getFieldValue(T t, String fieldName)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String requiredGetter = getter(fieldName);

        Method m = t.getClass().getMethod(requiredGetter);
        if (!m.isAccessible()) {
            m.setAccessible(true);
        }

        Object v = m.invoke(t);
        if (v instanceof String) {
            return (String)v;
        } else {
            return v.toString();
        }
    }

    private static String getter(String fieldName) {
        return "get" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
    }
}
