package snorochevskiy.pojoeval.evaluator;

import snorochevskiy.pojoeval.evaluator.exception.DslError;
import snorochevskiy.pojoeval.evaluator.exception.EvalException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class RuleEvaluatorTest {

    @Test
    public void textSimpleEqRule() {
        String rule = " grade = \"Junior\" ";

        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = new RuleEvaluator<>(rule, Programmer.class);
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void textSimpleEqRuleSingleQuotes() {
        String rule = " grade = 'Junior' ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = new RuleEvaluator<>(rule, Programmer.class);
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void textSimpleNotEqRule() {
        String rule = " grade != \"Senior\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = new RuleEvaluator<>(rule, Programmer.class);
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void textSimpleNotRule() {
        String rule = "NOT grade = \"Senior\"";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = new RuleEvaluator<>(rule, Programmer.class);
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void textDoubleNotRule() {
        String rule = "NOT NOT grade = \"Junior\"";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = new RuleEvaluator<>(rule, Programmer.class);
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void textSimpleContainsRule() {
        String rule = " position contains \"engineer\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = new RuleEvaluator<>(rule, Programmer.class);
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void textSimpleContainsRegexpRule() {
        String rule = " location contains_regexp \"Room\\d{2}\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = new RuleEvaluator<>(rule, Programmer.class);
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void textSimpleMatchesRule() {
        String rule = "location matches \"^Office\\d+-Room\\d+$\"";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = new RuleEvaluator<>(rule, Programmer.class);
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void textSimpleInRule() {
        String rule = "firstName IN [\"John\", \"Michael\",\"Robert\"]";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = new RuleEvaluator<>(rule, Programmer.class);
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void textSyntacticError() {
        String rule = "firstName unsupportedfunc \"ololo\"";

        Exception e = null;
        try {
            new RuleEvaluator<>(rule, Programmer.class);
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
    public void textOrEqRule() {
        String rule = " firstName = \"AAA\" OR lastName= \"Doe\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = new RuleEvaluator<>(rule, Programmer.class);
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void textAndEqRule() {
        String rule = " firstName = \"John\" AND lastName= \"Doe\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = new RuleEvaluator<>(rule, Programmer.class);
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void textNestedEqRule() {
        String rule = " lastName = \"Doe\" AND NOT ( firstName = \"Robert\" )";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        RuleEvaluator<Programmer> evaluator = new RuleEvaluator<>(rule, Programmer.class);
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void textSimpleEqRuleWithExtractor() {
        String rule = " fullName = \"John Doe\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Map<String, Function<Programmer,Object>> extractors = Collections.singletonMap("fullName",
                (Programmer p)->p.getFirstName() + " " + p.getLastName());
        RuleEvaluator<Programmer> evaluator = new RuleEvaluator<>(rule, Programmer.class, extractors);
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testWithoutFieldsValidation() {
        class Book {
            public String getTitle() { return "The Horus Heresy"; }
        }

        String rule = " title = 'The Horus Heresy' ";
        RuleEvaluator<Object> evaluator = new RuleEvaluator<>(rule);
        boolean result = evaluator.evaluate(new Book());

        Assert.assertTrue(result);
    }

    @Test(expected = EvalException.class)
    public void testWithoutFieldsValidationFieldException() {
        String rule = " missingField = 'ololo' ";
        RuleEvaluator<Object> evaluator = new RuleEvaluator<>(rule);
        boolean result = evaluator.evaluate(new Object());

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
