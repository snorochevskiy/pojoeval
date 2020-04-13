package snorochevskiy.pojoeval.v2.reflect;

import snorochevskiy.pojoeval.v2.evaluator.ExprResType;
import snorochevskiy.pojoeval.v2.util.Opt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;

public class ReflectionUtils {

    private static final Class[] NUM_PRIMITIVES = new Class[]{
            byte.class, short.class, int.class, long.class, float.class, double.class
    };

    private ReflectionUtils() {

    }

    /**
     * Searches for a getter method for a field with given name, and returns it's type.
     * Field name can a dot-separated path that identifies a nested field.
     *
     * @param cls
     * @param fieldName
     * @return
     */
    public static Optional<Class<?>> getFieldPathType(Class<?> cls, String fieldName) {

        BiFunction<Optional<Class<?>>, String, Optional<Class<?>>> reducer =
                (opCls, name) -> opCls.flatMap((Class<?> c) -> ReflectionUtils.getFieldType(c, name));

        return Arrays.stream(fieldName.split("\\."))
                .reduce(Optional.ofNullable(cls), reducer, (a,b) -> a);
    }


    public static boolean hasFieldPath(Class<?> cls, String fieldName) {
        return getFieldPathType(cls, fieldName).isPresent();
    }

    public static Optional<Class<?>> getFieldType(Class cls, String fieldName) {
        String requiredGetter = getter(fieldName);
        return Arrays.stream(cls.getMethods())
                .filter(m -> requiredGetter.equals(m.getName()) && m.getParameterCount() == 0)
                .findAny()
                .map(Method::getReturnType);
    }

    public static Optional<ExprResType> getFieldExprType(Class cls, String fieldName) {
        String requiredGetter = getter(fieldName);
        return Arrays.stream(cls.getMethods())
                .filter(m -> requiredGetter.equals(m.getName()) && m.getParameterCount() == 0)
                .findAny()
                .map(m-> toExprType(m.getReturnType()));
    }

    public static <T> Opt<Object> getFieldPathValue(T t, String fieldName) {

        BiFunction<Opt<Object>, String, Opt<Object>> reducer =
                (optObj, name) -> optObj.flatMapNonNullable((Object o) -> ReflectionUtils.getFieldValueOpt(o, name));

        return Arrays.stream(fieldName.split("\\."))
                .reduce(Opt.of(t), reducer, (a,b) -> a);
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
