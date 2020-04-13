package snorochevskiy.pojoeval.v2.reflect;

import org.junit.Assert;
import org.junit.Test;
import snorochevskiy.pojoeval.v2.util.Opt;

public class ReflectionUtilsTest {

    @Test
    public void testCheckIfNestedFieldExists() {
        Assert.assertTrue(ReflectionUtils.hasFieldPath(Cat.class, "head.eyeColor"));
    }

    @Test
    public void testGetNestedFieldValue() {
        // given
        Cat cat = new Cat(new Head(EyeColor.YELLOW));

        // when
        Opt<Object> optV = ReflectionUtils.getFieldPathValue(cat, "head.eyeColor");

        Assert.assertTrue(optV.isDefined());
        Assert.assertEquals(EyeColor.YELLOW, optV.get());
    }

    class Head {
        private EyeColor eyeColor;

        public Head(EyeColor eyeColor) {
            this.eyeColor = eyeColor;
        }

        public EyeColor getEyeColor() {
            return eyeColor;
        }

        public void setEyeColor(EyeColor eyeColor) {
            this.eyeColor = eyeColor;
        }
    }

    class Cat {
        private Head head;

        public Cat(Head head) {
            this.head = head;
        }

        public Head getHead() {
            return head;
        }

        public void setHead(Head head) {
            this.head = head;
        }
    }

    enum EyeColor {
        YELLOW, BLUE, GREEN, BROWN
    }
}
