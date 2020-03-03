package snorochevskiy.pojoeval.v1.evaluator;

import java.util.Map;
import java.util.function.Function;

public class EvaluationContext<POJO> {
    private Map<String, Function<POJO,Object>> fieldExtractorsMap;
    private ExternalFieldsExtractor<POJO> externalFieldsExtractor;

    public EvaluationContext(Map<String, Function<POJO, Object>> fieldExtractorsMap,
                             ExternalFieldsExtractor<POJO> externalFieldsExtractor) {
        this.fieldExtractorsMap = fieldExtractorsMap;
        this.externalFieldsExtractor = externalFieldsExtractor;
    }

    public Map<String, Function<POJO, Object>> getFieldExtractorsMap() {
        return fieldExtractorsMap;
    }

    public ExternalFieldsExtractor<POJO> getExternalFieldsExtractor() {
        return externalFieldsExtractor;
    }
}
