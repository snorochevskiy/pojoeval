package snorochevskiy.pojoeval.v2.evaluator;

import org.junit.Assert;
import org.junit.Test;
import snorochevskiy.pojoeval.v2.evaluator.pojos.Programmer;

import java.util.ArrayList;

public class SimpleLogicalTest {

    @Test
    public void testQeTwoString() {
        String rule = " 'aaa' = 'aaa' ";

        Evaluator<?, Boolean> evaluator = Evaluator.createForRule(rule)
                .buildBoolEvaluator();
        Object result = evaluator.evaluate(null);

        System.out.println(result);
    }

    @Test
    public void testSimpleEqRule() {
        String rule = " grade = 'Junior' ";

        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Evaluator<Programmer, Boolean> evaluator = Evaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .buildBoolEvaluator();

        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testSimpleEqRuleSingleQuotes() {
        String rule = " grade = 'Junior' ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Evaluator<Programmer, Boolean> evaluator = Evaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .buildBoolEvaluator();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testSimpleNotEqRule() {
        String rule = " grade != \"Senior\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Evaluator<Programmer, Boolean> evaluator = Evaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .buildBoolEvaluator();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testSimpleNotRule() {
        String rule = "NOT grade = \"Senior\"";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Evaluator<Programmer,Boolean> evaluator = Evaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .buildBoolEvaluator();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testDoubleNotRule() {
        String rule = "NOT NOT grade = \"Junior\"";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Evaluator<Programmer, Boolean> evaluator = Evaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .buildBoolEvaluator();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

}
