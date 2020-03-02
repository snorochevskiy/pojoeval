package snorochevskiy.pojoeval.evaluator;

import snorochevskiy.pojoeval.evaluator.exception.DslError;
import snorochevskiy.pojoeval.evaluator.exception.EvalException;
import org.junit.Assert;
import org.junit.Test;
import snorochevskiy.pojoeval.util.opt.Opt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class RuleEvaluatorTest {

    @Test
    public void testSimpleEqRule() {
        String rule = " grade = \"Junior\" ";

        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .build();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testSimpleEqRuleSingleQuotes() {
        String rule = " grade = 'Junior' ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .build();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testSimpleNotEqRule() {
        String rule = " grade != \"Senior\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .build();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testSimpleNotRule() {
        String rule = "NOT grade = \"Senior\"";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .build();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testDoubleNotRule() {
        String rule = "NOT NOT grade = \"Junior\"";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .build();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testSimpleContainsRule() {
        String rule = " position contains \"engineer\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .build();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testSimpleContainsRegexpRule() {
        String rule = " location contains_regexp \"Room\\d{2}\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .build();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testSimpleMatchesRule() {
        String rule = "location matches \"^Office\\d+-Room\\d+$\"";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .build();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testSimpleInRule() {
        String rule = "firstName IN [\"John\", \"Michael\",\"Robert\"]";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .build();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

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
    public void testOrEqRule() {
        String rule = " firstName = \"AAA\" OR lastName= \"Doe\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .build();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testAndEqRule() {
        String rule = " firstName = \"John\" AND lastName= \"Doe\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .build();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testNestedEqRule() {
        String rule = " lastName = \"Doe\" AND NOT ( firstName = \"Robert\" )";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = RuleEvaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .build();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
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
        boolean result = evaluator.evaluate(pojo);

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
        boolean result = evaluator.evaluate(new Book());

        Assert.assertTrue(result);
    }

    @Test(expected = EvalException.class)
    public void testWithoutFieldsValidationFieldException() {
        String rule = " missingField = 'ololo' ";
        RuleEvaluator<Object> evaluator = RuleEvaluator.createForRule(rule)
                .build();
        boolean result = evaluator.evaluate(new Object());

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

        boolean result = evaluator.evaluate(pojo, context);

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

        boolean result = evaluator.evaluate(pojo, context);

        Assert.assertTrue(result);
    }

    class Programmer {
        private String firstName; // "John"
        private String lastName; // "Doe"
        private String birthData; // DD MM YYYY
        private String location; // "Office1-Room2", "Office5-Room19"

        private String grade; // "Junior", "Middle", "Senior", etc.
        private String position; // "Software engineer", "Network engineer", "Business analysts"
        private String academicDegree; // "None", "Bachelor", "Master", "PhD"
        private List<String> skills; // ["C++", "Haskell", "Erlang"]

        public Programmer(String firstName, String lastName, String birthData, String location, String grade,
                          String position, String academicDegree, List<String> skills) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.birthData = birthData;
            this.location = location;
            this.grade = grade;
            this.position = position;
            this.academicDegree = academicDegree;
            this.skills = skills;
        }

        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getBirthData() { return birthData; }
        public String getLocation() { return location; }
        public String getGrade() { return grade; }
        public String getPosition() { return position; }
        public String getAcademicDegree() { return academicDegree; }
        public List<String> getSkills() { return skills; }
    }

}
