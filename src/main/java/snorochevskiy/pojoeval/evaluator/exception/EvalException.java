package snorochevskiy.pojoeval.evaluator.exception;

/**
 * Exception occurred during the evaluation on a POJO object.
 */
public class EvalException extends RuntimeException {

    public EvalException(String msg) {
        super(msg);
    }
}
