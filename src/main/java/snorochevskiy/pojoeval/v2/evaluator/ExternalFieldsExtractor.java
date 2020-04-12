package snorochevskiy.pojoeval.v2.evaluator;

import snorochevskiy.pojoeval.v2.util.Opt;

public interface ExternalFieldsExtractor<POJO> {
    Opt<Object> extractFieldValue(POJO pojo, String fieldName);
}
