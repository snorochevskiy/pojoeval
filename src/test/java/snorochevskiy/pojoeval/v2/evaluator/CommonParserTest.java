package snorochevskiy.pojoeval.v2.evaluator;

import snorochevskiy.pojoeval.v2.evaluator.exception.DslError;
import snorochevskiy.pojoeval.v2.evaluator.exception.EvalException;
import org.junit.Assert;
import org.junit.Test;
import snorochevskiy.pojoeval.v2.evaluator.pojos.Programmer;
import snorochevskiy.pojoeval.v2.util.Opt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public class CommonParserTest {

    @Test
    public void testSyntacticError() {
        String rule = "firstName unsupportedfunc \"ololo\"";

        Exception e = null;
        try {
            RuleEvaluator.<Programmer>createForRule(rule)
                    .validateAgainstClass(Programmer.class)
                    .build();
        } catch (Exception ex) {
            e = ex;
        }

        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof DslError);
        Assert.assertEquals("unsupportedfunc", ((DslError)e).getToken());
        Assert.assertEquals(10, ((DslError)e).getStartPos());
        Assert.assertEquals(24, ((DslError)e).getEndPos());
        Assert.assertEquals(1, ((DslError)e).getLine());
    }

    @Test
    public void testSimpleEqRuleWithExtractor() {
        String rule = " fullName = \"John Doe\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Map<String, Function<Programmer,Object>> extractors = Collections.singletonMap("fullName",
                (Programmer p)->p.getFirstName() + " " + p.getLastName());
        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .withFieldExtractors(extractors)
                .build();
        boolean result = evaluator.evaluateBool(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testWithoutFieldsValidation() {
        class Book {
            public String getTitle() { return "The Horus Heresy"; }
        }

        String rule = " title = 'The Horus Heresy' ";
        RuleEvaluator<Object> evaluator = RuleEvaluator.createForRule(rule)
                .build();
        //RuleEvaluator<Object> evaluator = new RuleEvaluator<>(rule);
        boolean result = evaluator.evaluateBool(new Book());

        Assert.assertTrue(result);
    }

    @Test(expected = EvalException.class)
    public void testWithoutFieldsValidationFieldException() {
        String rule = " missingField = 'ololo' ";
        RuleEvaluator<Object> evaluator = RuleEvaluator.createForRule(rule)
                .build();
        boolean result = evaluator.evaluateBool(new Object());

        Assert.assertTrue(result);
    }

    @Test(expected = DslError.class) //DslError{message='Cannot parse value', token='firstName', startPos=1, endPos=9, line=1}
    public void testNoUseReflection() {
        String rule = " firstName = 'John' ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .allowReflectionFieldLookup(false)
                .build();
    }

    @Test
    public void testUsingEvalContext_withExtractorFunctions() {
        String rule = " fullName = 'John Doe' ";

        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .allowReflectionFieldLookup(false)
                .build();

        Map<String, Function<Programmer,Object>> extractors = Collections.singletonMap("fullName",
                (Programmer p)->p.getFirstName() + " " + p.getLastName());
        EvaluationContext<Programmer> context = new EvaluationContext<>(extractors, null);

        boolean result = evaluator.evaluateBool(pojo, context);

        Assert.assertTrue(result);
    }

    @Test
    public void testUsingEvalContext_withExternalExtractor() {
        String rule = " fullName = 'John Doe' ";

        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .allowReflectionFieldLookup(false)
                .build();

        ExternalFieldsExtractor<Programmer> e = new ExternalFieldsExtractor<Programmer>() {
            @Override
            public Opt<Object> extractFieldValue(Programmer programmer, String fieldName) {
                if ("fullName".equals(fieldName)) {
                    return Opt.of(programmer.getFirstName() + " " + programmer.getLastName());
                }
                return Opt.empty();
            }
        };

        EvaluationContext<Programmer> context = new EvaluationContext<>(null, e);

        boolean result = evaluator.evaluateBool(pojo, context);

        Assert.assertTrue(result);
    }



}
