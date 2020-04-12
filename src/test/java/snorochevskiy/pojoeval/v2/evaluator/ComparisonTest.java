package snorochevskiy.pojoeval.v2.evaluator;

import org.junit.Assert;
import org.junit.Test;

public class ComparisonTest {

    @Test
    public void testGtTrue() {
        String rule = "5 > 2";

        Evaluator<?, Boolean> evaluator = Evaluator.createForRule(rule)
                .buildBoolEvaluator();

        Assert.assertEquals(ExprResType.BOOL, evaluator.getExpectedResultType());

        boolean b = evaluator.evaluate(null);
        Assert.assertTrue(b);
    }

    @Test
    public void testGtFalse() {
        String rule = "5 > 12.0";


        Evaluator<?, Boolean> evaluator = Evaluator.createForRule(rule)
                .buildBoolEvaluator();

        Assert.assertEquals(ExprResType.BOOL, evaluator.getExpectedResultType());

        boolean b = evaluator.evaluate(null);
        Assert.assertFalse(b);
    }

    @Test
    public void testWithFields() {
        String rule = "width > 5 AND height > 2";


        Evaluator<Rectangle, Boolean> evaluator = Evaluator.<Rectangle>createForRule(rule)
                .buildBoolEvaluator();

        Assert.assertEquals(ExprResType.BOOL, evaluator.getExpectedResultType());

        boolean b = evaluator.evaluate(new Rectangle(6, 3));
        Assert.assertTrue(b);
    }

    @Test
    public void testComplexComparison() {
        String rule = "width < height + 5";


        Evaluator<Rectangle, Boolean> evaluator = Evaluator.<Rectangle>createForRule(rule)
                .validateAgainstClass(Rectangle.class)
                .allowReflectionFieldLookup(true)
                .buildBoolEvaluator();

        Assert.assertEquals(ExprResType.BOOL, evaluator.getExpectedResultType());

        boolean b = evaluator.evaluate(new Rectangle(6, 3));
        Assert.assertTrue(b);
    }

    private static class Rectangle {
        private int width;
        private int height;

        public Rectangle(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }
    }
}
