package snorochevskiy.pojoeval.v2.evaluator.exception;

/**
 * Exception occurred during the evaluation on a POJO object.
 */
public class EvalException extends RuntimeException {

    public EvalException(String msg) {
        super(msg);
    }

    public EvalException(String msg, Throwable t) {
        super(msg, t);
    }
}
