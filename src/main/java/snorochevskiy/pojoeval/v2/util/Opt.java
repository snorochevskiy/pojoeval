package snorochevskiy.pojoeval.v2.util;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Standard Optional class doesn't allow to distinguish existing null value from the absence of a value.
 * @param <T>
 */
public class Opt<T> {

    private final T v;
    private final boolean defined;

    public Opt(T v) {
        this.v = v;
        defined = true;
    }

    public Opt() {
        v = null;
        defined = false;
    }

    public boolean isDefined() {
        return defined;
    }

    public boolean isNotDefined() {
        return !defined;
    }


    public T get() throws IllegalStateException {
        if (defined) {
            return v;
        } else {
            throw new IllegalStateException();
        }
    }

    public T getOrElse(T elseValue) {
        return defined ? v : elseValue;
    }

    public T getOrElse(Supplier<T> elseSupplier) {
        return defined ? v : elseSupplier.get();
    }

    public <U> Opt<U> mapNullable(Function<T,U> mapper) {
        return defined
                ? Opt.of(mapper.apply(v))
                : Opt.<U>empty();
    }

    public <U> Opt<U> mapNonNullable(Function<T,U> mapper) {
        return defined && v != null
                ? Opt.of(mapper.apply(v))
                : Opt.<U>empty();
    }

    public <U> Opt<U> flatMapNullable(Function<T,Opt<U>> mapper) {
        return defined
                ? mapper.apply(v)
                : Opt.<U>empty();
    }

    public <U> Opt<U> flatMapNonNullable(Function<T,Opt<U>> mapper) {
        return defined && v != null
                ? mapper.apply(v)
                : Opt.<U>empty();
    }

    public static <T> Opt<T> of(T v) {
        return new Opt<>(v);
    }

    public static <T> Opt<T> empty() {
        return new Opt<T>();
    }
}
