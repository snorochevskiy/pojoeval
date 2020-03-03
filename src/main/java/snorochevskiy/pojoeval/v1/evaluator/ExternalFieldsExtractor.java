package snorochevskiy.pojoeval.v1.evaluator;

import snorochevskiy.pojoeval.v1.opt.Opt;

public interface ExternalFieldsExtractor<POJO> {
    Opt<Object> extractFieldValue(POJO pojo, String fieldName);
}
