package snorochevskiy.pojoeval.v2.evaluator;

import org.junit.Assert;
import org.junit.Test;
import snorochevskiy.pojoeval.v2.evaluator.pojos.Programmer;

import java.util.ArrayList;

public class StringOperationsTest {

    @Test
    public void testSimpleContainsRule() {
        String rule = " position contains \"engineer\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Evaluator<Programmer, Boolean> evaluator = Evaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .buildBoolEvaluator();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testSimpleContainsRegexpRule() {
        String rule = " location contains_regexp \"Room\\d{2}\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Evaluator<Programmer, Boolean> evaluator = Evaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .buildBoolEvaluator();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testSimpleMatchesRule() {
        String rule = "location matches \"^Office\\d+-Room\\d+$\"";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Evaluator<Programmer, Boolean> evaluator = Evaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .buildBoolEvaluator();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

}
