package snorochevskiy.pojoeval.v2.reflect;

import snorochevskiy.pojoeval.v2.evaluator.ExprResType;
import snorochevskiy.pojoeval.v2.util.Opt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class ReflectionUtils {

    private static final Class[] NUM_PRIMITIVES = new Class[]{
            byte.class, short.class, int.class, long.class, float.class, double.class
    };

    private ReflectionUtils() {

    }

    public static boolean hasField(Class cls, String fieldName) {
        String requiredGetter = getter(fieldName);
        return Arrays.stream(cls.getMethods())
            .anyMatch(m -> requiredGetter.equals(m.getName()) && m.getParameterCount() == 0);
    }

    public static Optional<ExprResType> getFieldType(Class cls, String fieldName) {
        String requiredGetter = getter(fieldName);
        return Arrays.stream(cls.getMethods())
                .filter(m -> requiredGetter.equals(m.getName()) && m.getParameterCount() == 0)
                .findAny()
                .map(m-> toExprType(m.getReturnType()));
    }

    public static <T> Object getFieldValueOrNull(T t, String fieldName) {
        try {
            return getFieldValue(t, fieldName);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    public static <T> Opt<Object> getFieldValueOpt(T t, String fieldName) {
        try {
            return Opt.of(getFieldValue(t, fieldName));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return Opt.empty();
        }
    }

    public static <T> Object getFieldValue(T t, String fieldName)
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
            return v;
        }
    }

    private static String getter(String fieldName) {
        return "get" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
    }

    private static ExprResType toExprType(Class<?> cls) {
        if (cls == String.class) {
            return ExprResType.STR;
        } else if (isNumType(cls)) {
            return ExprResType.NUM;
        } else if (cls == boolean.class || cls == Boolean.class) {
            return ExprResType.OBJ;
        } else if (Collection.class.isAssignableFrom(cls)) { // what about arrays?
            return ExprResType.COLLECTION;
        } else {
            return ExprResType.UNKNOWN;
        }
    }

    private static boolean isNumType(Class<?> cls) {
        return Arrays.asList(NUM_PRIMITIVES).contains(cls) || Number.class.isAssignableFrom(cls);
    }
}
