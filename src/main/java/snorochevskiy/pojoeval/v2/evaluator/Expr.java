package snorochevskiy.pojoeval.v2.evaluator;

import java.io.Serializable;

public interface Expr<POJO> extends Serializable {

    ExprResType resultType();

    Object eval(POJO pojo, EvaluationContext<POJO> context);

    default boolean isBool() {
        return resultType() == ExprResType.BOOL || resultType() == ExprResType.UNKNOWN;
    }

    default boolean isNum() {
        return resultType() == ExprResType.NUM || resultType() == ExprResType.UNKNOWN;
    }

    default boolean isStr() {
        return resultType() == ExprResType.STR || resultType() == ExprResType.UNKNOWN;
    }

    default boolean isObj() {
        return resultType() == ExprResType.OBJ || resultType() == ExprResType.UNKNOWN;
    }
}
