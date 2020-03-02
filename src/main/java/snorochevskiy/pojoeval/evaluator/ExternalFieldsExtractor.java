package snorochevskiy.pojoeval.evaluator;

import snorochevskiy.pojoeval.util.opt.Opt;

public interface ExternalFieldsExtractor<POJO> {
    Opt<Object> extractFieldValue(POJO pojo, String fieldName);
}
