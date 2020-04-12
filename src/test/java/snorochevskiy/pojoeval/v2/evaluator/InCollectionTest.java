package snorochevskiy.pojoeval.v2.evaluator;

import org.junit.Assert;
import org.junit.Test;
import snorochevskiy.pojoeval.v2.evaluator.pojos.Programmer;

import java.util.ArrayList;

public class InCollectionTest {

    @Test
    public void testSimpleInRule() {
        String rule = "firstName IN [\"John\", \"Michael\",\"Robert\"]";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Evaluator<Programmer, Boolean> evaluator = Evaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .buildBoolEvaluator();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

}
