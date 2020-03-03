package snorochevskiy.pojoeval.v1.evaluator;

import java.io.Serializable;

@FunctionalInterface
public interface Expr<POJO, T> extends Serializable {
    T calc(POJO pojo, EvaluationContext<POJO> context);
}
