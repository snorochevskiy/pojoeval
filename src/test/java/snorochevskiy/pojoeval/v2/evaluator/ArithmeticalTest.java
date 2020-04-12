package snorochevskiy.pojoeval.v2.evaluator;

import org.junit.Assert;
import org.junit.Test;
import snorochevskiy.pojoeval.v2.evaluator.exception.DslError;
import snorochevskiy.pojoeval.v2.evaluator.pojos.Programmer;

public class ArithmeticalTest {

    @Test
    public void testJustNumber() {
        String rule = "5";


        Evaluator<?, Double> evaluator = Evaluator.createForRule(rule)
                .buildNumberEvaluator();

        Assert.assertEquals(ExprResType.NUM, evaluator.getExpectedResultType());

        Object o = evaluator.evaluate(null);
        Assert.assertTrue(o instanceof Number);
        Assert.assertEquals(5.0, o);
    }

    @Test
    public void testNegativeNumber() {
        String rule = "-5";


        Evaluator<?, Double> evaluator = Evaluator.createForRule(rule)
                .buildNumberEvaluator();

        Assert.assertEquals(ExprResType.NUM, evaluator.getExpectedResultType());

        Object o = evaluator.evaluate(null);
        Assert.assertTrue(o instanceof Number);
        Assert.assertEquals(-5.0, o);
    }

    @Test
    public void testAdd() {
        String rule = "5 + 5.0";


        Evaluator<?, Double> evaluator = Evaluator.createForRule(rule)
                .buildNumberEvaluator();

        Assert.assertEquals(ExprResType.NUM, evaluator.getExpectedResultType());

        Object o = evaluator.evaluate(null);
        Assert.assertTrue(o instanceof Number);
        Assert.assertEquals(10.0, o);
    }

    @Test(expected = DslError.class)
    public void testExceptionOnNonNumericArgument() {
        String rule = " 5 + 'ololo' ";


        Evaluator.<Programmer>createForRule(rule)
                .build();
    }

    @Test
    public void testDivideField() {
        String rule = "weight / 2 > 3";


        Evaluator<Product, Boolean> evaluator = Evaluator.<Product>createForRule(rule)
                .validateAgainstClass(Product.class)
                .buildBoolEvaluator();

        boolean b = evaluator.evaluate(new Product("Sofa", 10.0));
        Assert.assertTrue(b);
    }

    private static class Product {
        private String name;
        private double weight;

        public Product(String name, double weight) {
            this.name = name;
            this.weight = weight;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }
    }
}
