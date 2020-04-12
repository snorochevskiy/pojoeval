package snorochevskiy.pojoeval.v2.evaluator;

import org.junit.Assert;
import org.junit.Test;
import snorochevskiy.pojoeval.v2.evaluator.pojos.NetDeviceInfoMsg;
import snorochevskiy.pojoeval.v2.evaluator.pojos.Programmer;

import java.util.ArrayList;

public class ComplexLogicalTest {

    @Test
    public void testOrEqRule() {
        String rule = " firstName = \"AAA\" OR lastName= \"Doe\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Evaluator<Programmer, Boolean> evaluator = Evaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .buildBoolEvaluator();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testAndEqRule() {
        String rule = " firstName = \"John\" AND lastName= \"Doe\" ";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Evaluator<Programmer, Boolean> evaluator = Evaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .buildBoolEvaluator();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testNestedEqRule() {
        String rule = " lastName = \"Doe\" AND NOT ( firstName = \"Robert\" )";
        Programmer pojo = new Programmer("John", "Doe", "05 10 1970", "Office3-Room10", "Junior",
                "Software engineer" ,"Bachelor", new ArrayList<>());

        Evaluator<Programmer, Boolean> evaluator = Evaluator.<Programmer>createForRule(rule)
                .validateAgainstClass(Programmer.class)
                .buildBoolEvaluator();
        boolean result = evaluator.evaluate(pojo);

        Assert.assertTrue(result);
    }

    @Test
    public void testEqAndComparison() {
        String rule = " fqdn = 'device123.dc2.myisp.com' AND level > 2";

        Evaluator<NetDeviceInfoMsg, Boolean> evaluator = Evaluator.<NetDeviceInfoMsg>createForRule(rule)
                .validateAgainstClass(NetDeviceInfoMsg.class)
                .allowReflectionFieldLookup(true)
                .buildBoolEvaluator();

        NetDeviceInfoMsg msg = new NetDeviceInfoMsg("device123.dc2.myisp.com", "Eth10", "Aaaaa! Panic !!!", 5);

        boolean result = evaluator.evaluate(msg);
    }
}
