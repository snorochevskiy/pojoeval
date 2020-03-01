package snorochevskiy.pojoeval.evaluator;

import java.io.Serializable;

@FunctionalInterface
public interface Expr<MSG, T> extends Serializable {
    T calc(MSG msg);
}
