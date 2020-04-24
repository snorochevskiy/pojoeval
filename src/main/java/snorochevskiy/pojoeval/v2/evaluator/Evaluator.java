package snorochevskiy.pojoeval.v2.evaluator;

import snorochevskiy.pojoeval.v2.evaluator.exception.DslError;
import snorochevskiy.pojoeval.v2.evaluator.exception.EvalException;
import snorochevskiy.pojoeval.v2.reflect.ReflectionUtils;
import snorochevskiy.pojoeval.v2.util.Opt;
import org.antlr.v4.runtime.*;

import snorochevskiy.pojoeval.v2.dsl.parser.RuleDslLexer;
import snorochevskiy.pojoeval.v2.dsl.parser.RuleDslParser;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class represents an engine for executing DSL expressions on POJOs.
 * @param <POJO> class of POJO that rules are to be evaluated on. Used only for rules validation.
 */
public class Evaluator<POJO, R> implements Serializable {

    protected final Expr<POJO> compiled;
    protected final Class<POJO> msgClass;
    protected final Class<R> expectedResultType;
    protected Map<String, Function<POJO,Object>> fieldExtractors = new HashMap<>();
    protected boolean useReflection;

    private Evaluator(String rule, Class<POJO> msgClass, Class<R> expectedResultType, Map<String,
            Function<POJO,Object>> fieldExtractors, boolean useReflection) {
        this.msgClass = msgClass;
        this.expectedResultType = expectedResultType;
        this.fieldExtractors.putAll(fieldExtractors);
        this.useReflection = useReflection;

        RuleDslLexer lexer = new RuleDslLexer(CharStreams.fromString(rule));

        RuleDslParser parser = new RuleDslParser(new CommonTokenStream(lexer));
        BailErrorStrategy errHandler = new BailErrorStrategy();
        parser.setErrorHandler(errHandler);

        try {
            RuleDslParser.TopExprContext topExprContext = parser.topExpr();
            this.compiled = parseTopExpr(topExprContext);
        } catch (ParseCancellationException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RecognitionException) {
                RecognitionException recognitionException = (RecognitionException) cause;
                Token token = recognitionException.getOffendingToken();
                int startPos = token.getStartIndex();
                int endPos = token.getStopIndex();
                String errToken = token.getText();
                int line = token.getLine();
                throw new DslError("Syntax error", errToken, startPos, endPos, line);
            }
            throw new DslError("Unable to parse rule: " + e.getMessage(), rule, 0, 0, 0);
        }
    }

    /**
     * Evaluate rule on a given object.
     * @param pojo
     * @return
     */
    public R evaluate(POJO pojo) {
        Object res = compiled.eval(pojo, null);
        if (!expectedResultType.isAssignableFrom(res.getClass())) {
            throw new EvalException("");
        }
        return (R) res;
    }

    public R evaluate(POJO pojo, EvaluationContext context) {
        Object res = compiled.eval(pojo, context);
        if (!expectedResultType.isAssignableFrom(res.getClass())) {
            throw new EvalException("");
        }
        return (R) res;
    }

    public ExprResType getExpectedResultType() {
        return this.compiled.resultType();
    }

    Expr<POJO> parseTopExpr(RuleDslParser.TopExprContext topExprContext) {
        return parseLogicExpr(topExprContext.logicExpr());
    }

    Expr<POJO> parseLogicExpr(RuleDslParser.LogicExprContext logicExprContext) {
        return parseOrExpr(logicExprContext.orExpr());
    }

    Expr<POJO> parseOrExpr(RuleDslParser.OrExprContext orExprContext) {
        if (orExprContext.getChildCount() == 1) {
            return parseAndExpr(orExprContext.andExpr());
        } else if (orExprContext.getChildCount() == 3){
            Expr<POJO> e1 = parseOrExpr(orExprContext.orExpr());
            Expr<POJO> e2 = parseAndExpr(orExprContext.andExpr());
            return new OrExpr(e1, e2);
        } else {
            throw new DslError("Unable to parse expression", orExprContext);
        }
    }

    Expr<POJO> parseAndExpr(RuleDslParser.AndExprContext andExprContext) {
        if (andExprContext.getChildCount() == 1) {
            return parseNotExpr(andExprContext.notExpr());
        } else if (andExprContext.getChildCount() == 3) {
            Expr<POJO> e1 = parseAndExpr(andExprContext.andExpr());
            Expr<POJO> e2 = parseNotExpr(andExprContext.notExpr());
            return new AndExpr(e1, e2);
        } else {
            throw new DslError("Unable to parse expression", andExprContext);
        }
    }

    Expr<POJO> parseNotExpr(RuleDslParser.NotExprContext notExprContext) {
        if (notExprContext.getChildCount() == 1) { // fallthrough
            return parseEqExpr(notExprContext.eqExpr());
        } if (notExprContext.getChildCount() == 2) { // NOT expr
            Expr<POJO> e = parseNotExpr(notExprContext.notExpr());
            return new NotExpr(e);
        } else {
            throw new DslError("Unable to parse expression", notExprContext);
        }
    }

    Expr<POJO> parseEqExpr(RuleDslParser.EqExprContext eqExprContext) {
        if (eqExprContext.getChildCount() == 1 && !eqExprContext.additiveExpr().isEmpty()) {
            return parseAdditiveExpr(eqExprContext.additiveExpr(0));
        } else if (eqExprContext.getChildCount() == 1) {
            throw new DslError("Unexpected token while parsing Eq expression", eqExprContext);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.Eq() != null) {
            Expr<POJO> e1 = parseEqExpr(eqExprContext.eqExpr());
            Expr<POJO> e2 = parseAdditiveExpr(eqExprContext.additiveExpr(0));
            return new EqExpr(e1, e2);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.NEq() != null) {
            Expr<POJO> e1 = parseEqExpr(eqExprContext.eqExpr());
            Expr<POJO> e2 = parseAdditiveExpr(eqExprContext.additiveExpr(0));
            return new NotEqExpr(e1, e2);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.StrContains() != null) {
            Expr<POJO> e1 = parseRelExpr(eqExprContext.relExpr(0));
            Expr<POJO> e2 = parseRelExpr(eqExprContext.relExpr(1));
            if (!e1.isStr() || !e2.isStr()) {
                throw new DslError("Operation 'contains' can operate on string only", eqExprContext);
            }
            return new StrContainsExpr(e1, e2);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.StrContainsRegexp() != null) {
            Expr<POJO> e = parseRelExpr(eqExprContext.relExpr(0));
            String regex = parseTextFromRel(eqExprContext.relExpr(1));
            if (!e.isStr()) {
                throw new DslError("Operation 'contains' can operate on string only", eqExprContext);
            }
            try {
                Pattern.compile(regex);
            } catch (Exception exp) {
                throw new DslError("Bad regular expression: " + regex, eqExprContext);
            }
            return new ContainsRegexpExpr(e, regex);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.StrMatches() != null) {
            Expr<POJO> e = parseRelExpr(eqExprContext.relExpr(0));
            String regex = parseTextFromRel(eqExprContext.relExpr(1));
            if (!e.isStr()) {
                throw new DslError("Operation 'contains' can operate on string only", eqExprContext);
            }
            try {
                Pattern.compile(regex);
            } catch (Exception exp) {
                throw new DslError("Bad regular expression: " + regex, eqExprContext);
            }
            return new MatchExpr(e, regex);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.In() != null) {
            Expr<POJO> e = parseTextExpr(eqExprContext.relExpr(0));
            List<String> stringList = parseStringList(eqExprContext.stringList());
            return new InExpr(e, stringList);
        } else if (eqExprContext.Compare() != null) {
            Expr<POJO> e1 = parseAdditiveExpr(eqExprContext.additiveExpr(0));
            Expr<POJO> e2 = parseAdditiveExpr(eqExprContext.additiveExpr(1));
            if (!e1.isNum() || !e2.isNum()) {
                throw new DslError("Only numbers can be compared", eqExprContext);
            }
            return new CompareExpression(e1, e2, eqExprContext.Compare().getText());
        }

        throw new DslError("Unexpected expression", eqExprContext);
    }

    Expr<POJO> parseAdditiveExpr(RuleDslParser.AdditiveExprContext additiveExprContext) {
        if (additiveExprContext.getChildCount() == 1 && additiveExprContext.multiplicativeExpr() != null) {
            return parseMultiplicativeExpr(additiveExprContext.multiplicativeExpr());
        } else if (additiveExprContext.getChildCount() == 1) {
            throw new DslError("Unexpected token while parsing expression", additiveExprContext);
        } else if (additiveExprContext.getChildCount() == 3 && additiveExprContext.Plus() != null) {
            Expr<POJO> e1 = parseAdditiveExpr(additiveExprContext.additiveExpr());
            Expr<POJO> e2 = parseMultiplicativeExpr(additiveExprContext.multiplicativeExpr());
            if (!e1.isNum() || !e2.isNum()) {
                throw new DslError("Non numeric arguments for arithmetic operation", additiveExprContext);
            }
            return new AddExpr(e1, e2);
        } else if (additiveExprContext.getChildCount() == 3 && additiveExprContext.Minus() != null) {
            Expr<POJO> e1 = parseAdditiveExpr(additiveExprContext.additiveExpr());
            Expr<POJO> e2 = parseMultiplicativeExpr(additiveExprContext.multiplicativeExpr());
            if (!e1.isNum() || !e2.isNum()) {
                throw new DslError("Non numeric arguments for arithmetic operation", additiveExprContext);
            }
            return new SubtractExpr(e1, e2);
        }

        throw new DslError("Unexpected expression", additiveExprContext);
    }

    Expr<POJO> parseMultiplicativeExpr(RuleDslParser.MultiplicativeExprContext multiplicativeExprContext) {
        if (multiplicativeExprContext.getChildCount() == 1 && multiplicativeExprContext.relExpr() != null) {
            return parseRelExpr(multiplicativeExprContext.relExpr());
        } else if (multiplicativeExprContext.getChildCount() == 1) {
            throw new DslError("Unexpected token while parsing expression", multiplicativeExprContext);
        } else if (multiplicativeExprContext.getChildCount() == 3 && multiplicativeExprContext.Multiply() != null) {
            Expr<POJO> e1 = parseMultiplicativeExpr(multiplicativeExprContext.multiplicativeExpr());
            Expr<POJO> e2 = parseRelExpr(multiplicativeExprContext.relExpr());
            if (!e1.isNum() || !e2.isNum()) {
                throw new DslError("Non numeric arguments for arithmetic operation", multiplicativeExprContext);
            }
            return new MultiplyExpr(e1, e2);
        } else if (multiplicativeExprContext.getChildCount() == 3 && multiplicativeExprContext.Divide() != null) {
            Expr<POJO> e1 = parseMultiplicativeExpr(multiplicativeExprContext.multiplicativeExpr());
            Expr<POJO> e2 = parseRelExpr(multiplicativeExprContext.relExpr());
            if (!e1.isNum() || !e2.isNum()) {
                throw new DslError("Non numeric arguments for arithmetic operation", multiplicativeExprContext);
            }
            return new DivideExpr(e1, e2);
        } else if (multiplicativeExprContext.getChildCount() == 3 && multiplicativeExprContext.Mod() != null) {
            Expr<POJO> e1 = parseMultiplicativeExpr(multiplicativeExprContext.multiplicativeExpr());
            Expr<POJO> e2 = parseRelExpr(multiplicativeExprContext.relExpr());
            if (!e1.isNum() || !e2.isNum()) {
                throw new DslError("Non numeric arguments for arithmetic operation", multiplicativeExprContext);
            }
            return new ModuloExpr(e1, e2);
        }

        throw new DslError("Unexpected expression", multiplicativeExprContext);
    }

    Expr<POJO> parseRelExpr(RuleDslParser.RelExprContext relExprContext) {
        if (relExprContext.StringLiteral() != null) {
            String literal = relExprContext.StringLiteral().getText();
            // removing quotes
            String txt = literal.substring(1, literal.length() - 1);
            if (literal.charAt(0) == '\'') {
                txt = txt.replace("\\'", "'");
            }
            if (literal.charAt(0) == '"') {
                txt = txt.replace("\\\"", "\"");
            }
            return new LiteralExpr(txt);
        } else  if (relExprContext.DigitSequence() != null) {
            String numStr = relExprContext.DigitSequence().getText();
            try {
                return new NumberExpr(Double.parseDouble(numStr));
            } catch (Exception e) {
                throw new DslError("Cannot parse a number from", relExprContext);
            }
        } else if (relExprContext.Identifier() != null) {
            return parseFieldExpr(relExprContext);
        } else if (relExprContext.OpBr() != null && relExprContext.ClBr() != null) {
            return parseLogicExpr(relExprContext.logicExpr());
        }
        throw new DslError("Unexpected expression", relExprContext);
    }

    Expr<POJO> parseTextExpr(RuleDslParser.RelExprContext relExprContext) {
        if (relExprContext.getChildCount() == 1) {
            if (relExprContext.Identifier() != null) {
                String identifierName = relExprContext.Identifier().getText();
                if (fieldExtractors.containsKey(identifierName)
                        || msgClass != null && useReflection && ReflectionUtils.hasFieldPath(msgClass, identifierName)
                        || msgClass == null) {
                    return new FieldExpr(identifierName);
                } else {
                    throw new DslError("Cannot parse value", relExprContext.Identifier().getSymbol());
                }
            } else if (relExprContext.StringLiteral() != null) {
                String literal = relExprContext.StringLiteral().getText();
                return new LiteralExpr(literal.substring(1, literal.length() - 1));
            }
        }
        throw new DslError("Unable to parse expression", relExprContext);
    }

    String parseTextFromRel(RuleDslParser.RelExprContext relExprContext) {
        if (relExprContext.getChildCount() == 1) {
            if (relExprContext.StringLiteral() != null) {
                String literal = relExprContext.StringLiteral().getText();
                return literal.substring(1, literal.length() - 1);
            }
        }
        throw new DslError("Unable to parse expression", relExprContext);
    }

    List<String> parseStringList(RuleDslParser.StringListContext stringListContext) {
        return stringListContext.StringLiteral().stream()
                .map(ParseTree::getText)
                .map(literal -> literal.substring(1, literal.length() - 1))
                .collect(Collectors.toList());
    }

    FieldExpr parseFieldExpr(RuleDslParser.RelExprContext relExprContext) {
        String identifierName = relExprContext.Identifier().getText();

        if (fieldExtractors.containsKey(identifierName)) {
            // TODO: try to get field type from extractor function
            return new FieldExpr(identifierName);
        }
        if (msgClass != null && useReflection && ReflectionUtils.hasFieldPath(msgClass, identifierName)) {
            ExprResType resType = ReflectionUtils.getFieldExprType(msgClass, identifierName).get();
            return new FieldExpr(identifierName, resType);
        }
        if (msgClass == null) {
            return new FieldExpr(identifierName);
        }

        throw new DslError("Cannot parse value from ", relExprContext.Identifier().getSymbol());

    }

    private class OrExpr implements Expr<POJO> {

        private final Expr<POJO> e1;
        private final Expr<POJO> e2;

        private OrExpr(Expr<POJO> e1, Expr<POJO> e2) {
            this.e1 = e1;
            this.e2 = e2;
        }

        @Override
        public ExprResType resultType() {
            return ExprResType.BOOL;
        }

        @Override
        public Object eval(POJO pojo, EvaluationContext<POJO> context) {
            Object v1 = e1.eval(pojo, context);
            if (!(v1 instanceof Boolean)) {
                throw new EvalException("Expected boolean but got " + v1.getClass().getName());
            }
            boolean v1Bool = (Boolean) v1;
            if (v1Bool) {
                return true;
            }
            Object v2 = e2.eval(pojo, context);
            if (!(v2 instanceof Boolean)) {
                throw new EvalException("Expected boolean but got " + v2.getClass().getName());
            }
            return v2;
        }
    }

    private class AndExpr implements Expr<POJO> {

        private final Expr<POJO> e1;
        private final Expr<POJO> e2;

        private AndExpr(Expr<POJO> e1, Expr<POJO> e2) {
            this.e1 = e1;
            this.e2 = e2;
        }

        @Override
        public ExprResType resultType() {
            return ExprResType.BOOL;
        }

        @Override
        public Boolean eval(POJO pojo, EvaluationContext<POJO> context) {
            Object v1 = e1.eval(pojo, context);
            if (!(v1 instanceof Boolean)) {
                throw new EvalException("Expected boolean but got " + v1.getClass().getName());
            }
            boolean v1Bool = (Boolean) v1;

            Object v2 = e2.eval(pojo, context);
            if (!(v2 instanceof Boolean)) {
                throw new EvalException("Expected boolean but got " + v1.getClass().getName());
            }
            boolean v2Bool = (Boolean) v1;

            return v1Bool && v2Bool;
        }
    }

    private class NotExpr implements Expr<POJO> {

        private final Expr<POJO> e;

        private NotExpr(Expr<POJO> e) {
            this.e = e;
        }

        @Override
        public ExprResType resultType() {
            return ExprResType.BOOL;
        }

        @Override
        public Boolean eval(POJO pojo, EvaluationContext<POJO> context) {
            Object v = e.eval(pojo, context);
            if (!(v instanceof Boolean)) {
                throw new EvalException("Expected boolean but got " + v.getClass().getName());
            }
            return !((Boolean)v);
        }
    }

    private class EqExpr<T> implements Expr<POJO> {
        private final Expr<POJO> arg1;
        private final Expr<POJO> arg2;

        public EqExpr(Expr<POJO> arg1, Expr<POJO> arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public ExprResType resultType() {
            return ExprResType.BOOL;
        }

        @Override
        public Boolean eval(POJO pojo, EvaluationContext<POJO> context) {
            Object v1 = arg1.eval(pojo, context);
            Object v2 = arg2.eval(pojo, context);

            if (v1 == null) {
                return v2 == null || "null".equals(v2);
            }

            return v1.equals(v2);
        }
    }

    private class NotEqExpr<T> implements Expr<POJO> {
        private final Expr<POJO> arg1;
        private final Expr<POJO> arg2;

        public NotEqExpr(Expr<POJO> arg1, Expr<POJO> arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public ExprResType resultType() {
            return ExprResType.BOOL;
        }

        @Override
        public Boolean eval(POJO pojo, EvaluationContext<POJO> context) {
            Object v1 = arg1.eval(pojo, context);
            Object v2 = arg2.eval(pojo, context);
            return v1 != null
                    ? !v1.equals(v2)
                    : v2 == null;
        }
    }

    private class StrContainsExpr implements Expr<POJO> {
        private final Expr<POJO> arg1;
        private final Expr<POJO> arg2;

        public StrContainsExpr(Expr<POJO> arg1, Expr<POJO> arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public ExprResType resultType() {
            return ExprResType.BOOL;
        }

        @Override
        public Boolean eval(POJO pojo, EvaluationContext<POJO> context) {
            Object o1 = arg1.eval(pojo, context);
            if (o1 == null) {
                return false;
            }
            if (!(o1 instanceof String)) {
                throw new EvalException("Expected string but got " + o1.getClass().getName());
            }
            String s1 = (String) o1;

            Object o2 = arg2.eval(pojo, context);
            if (o2 == null) {
                return false;
            }
            if (!(o2 instanceof String)) {
                throw new EvalException("Expected string but got " + o1.getClass().getName());
            }
            String s2 = (String) o2;

            return s1.contains(s2);
        }
    }

    private class ContainsRegexpExpr implements Expr<POJO> {
        private final Expr<POJO> arg1;
        private final String regexp;
        private final Pattern pattern;

        public ContainsRegexpExpr(Expr<POJO> arg1, String regexp) {
            this.arg1 = arg1;
            this.regexp = regexp;
            this.pattern = Pattern.compile(regexp);
        }

        @Override
        public ExprResType resultType() {
            return ExprResType.BOOL;
        }

        @Override
        public Boolean eval(POJO pojo, EvaluationContext<POJO> context) {
            Object o = arg1.eval(pojo, context);

            if (o == null) {
                return false;
            }
            if (!(o instanceof String)) {
                throw new EvalException("Expected string but got " + o.getClass().getName());
            }
            String s = (String)o;

            Matcher m = pattern.matcher(s);
            boolean result = m.find();
            return result;
        }
    }

    private class MatchExpr implements Expr<POJO> {
        private final Expr<POJO> arg1;
        private final String regexp;
        private final Pattern pattern;

        public MatchExpr(Expr<POJO> arg1, String regexp) {
            this.arg1 = arg1;
            this.regexp = regexp;
            this.pattern = Pattern.compile(regexp);
        }

        @Override
        public ExprResType resultType() {
            return ExprResType.BOOL;
        }

        @Override
        public Boolean eval(POJO pojo, EvaluationContext<POJO> context) {
            Object o = arg1.eval(pojo, context);

            if (o == null) {
                return false;
            }
            if (!(o instanceof String)) {
                throw new EvalException("Expected string but got " + o.getClass().getName());
            }
            String s = (String)o;

            Matcher m = pattern.matcher(s);
            boolean result = m.matches();
            return result;
        }
    }

    private class InExpr implements Expr<POJO> {
        private final Expr<POJO> fieldExpr;
        private final List<String> stringList;

        private InExpr(Expr<POJO> field, List<String> stringList) {
            this.fieldExpr = field;
            this.stringList = stringList;
        }

        @Override
        public ExprResType resultType() {
            return ExprResType.BOOL;
        }

        @Override
        public Boolean eval(POJO pojo, EvaluationContext<POJO> context) {
            Object o = fieldExpr.eval(pojo, context);
            if (o == null) {
                return false;
            }
            if (!(o instanceof String)) {
                throw new EvalException("Expected string but got " + o.getClass().getName());
            }

            String s = (String)o;
            return stringList.contains(s);
        }
    }

    private abstract class BinaryArithmeticExpr implements Expr<POJO> {

        private final Expr<POJO> arg1;
        private final Expr<POJO> arg2;

        public BinaryArithmeticExpr(Expr<POJO> arg1, Expr<POJO> arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public ExprResType resultType() {
            return ExprResType.NUM;
        }

        @Override
        public Object eval(POJO pojo, EvaluationContext<POJO> context) {
            Object o1 = arg1.eval(pojo, context);
            if (o1 == null) {
                throw new EvalException("Left argument is null");
            }
            if (!(o1 instanceof Number)) {
                throw new EvalException("Left argument should be number, but it is " + o1.getClass().getName());
            }
            Number n1 = (Number) o1;

            Object o2 = arg2.eval(pojo, context);
            if (o2 == null) {
                throw new EvalException("Left argument is null");
            }
            if (!(o2 instanceof Number)) {
                throw new EvalException("Left argument should be number, but it is " + o2.getClass().getName());
            }
            Number n2 = (Number) o2;

            return arithmCalc(n1, n2);
        }

        protected abstract Number arithmCalc(Number n1, Number n2);
    }

    private class AddExpr extends BinaryArithmeticExpr {

        public AddExpr(Expr<POJO> arg1, Expr<POJO> arg2) {
            super(arg1, arg2);
        }

        @Override
        protected Number arithmCalc(Number n1, Number n2) {
            return n1.doubleValue() + n2.doubleValue();
        }
    }

    private class SubtractExpr extends BinaryArithmeticExpr {

        public SubtractExpr(Expr<POJO> arg1, Expr<POJO> arg2) {
            super(arg1, arg2);
        }

        @Override
        protected Number arithmCalc(Number n1, Number n2) {
            return n1.doubleValue() - n2.doubleValue();
        }
    }

    private class MultiplyExpr extends BinaryArithmeticExpr {

        public MultiplyExpr(Expr<POJO> arg1, Expr<POJO> arg2) {
            super(arg1, arg2);
        }

        @Override
        protected Number arithmCalc(Number n1, Number n2) {
            return n1.doubleValue() * n2.doubleValue();
        }
    }

    private class DivideExpr extends BinaryArithmeticExpr {

        public DivideExpr(Expr<POJO> arg1, Expr<POJO> arg2) {
            super(arg1, arg2);
        }

        @Override
        protected Number arithmCalc(Number n1, Number n2) {
            if (n2.doubleValue() == 0) {
                throw new EvalException("Cannot divide by zero");
            }
            return n1.doubleValue() / n2.doubleValue();
        }
    }

    private class ModuloExpr extends BinaryArithmeticExpr {

        public ModuloExpr(Expr<POJO> arg1, Expr<POJO> arg2) {
            super(arg1, arg2);
        }

        @Override
        protected Number arithmCalc(Number n1, Number n2) {
            if (n2.doubleValue() == 0) {
                throw new EvalException("Cannot divide by zero");
            }
            return n1.doubleValue() % n2.doubleValue();
        }
    }

    private class CompareExpression implements Expr<POJO> {

        private final Expr<POJO> arg1;
        private final Expr<POJO> arg2;
        private String operator;

        public CompareExpression(Expr<POJO> arg1, Expr<POJO> arg2, String operator) {
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.operator = operator;
        }

        @Override
        public ExprResType resultType() {
            return ExprResType.BOOL;
        }

        @Override
        public Object eval(POJO pojo, EvaluationContext<POJO> context) {
            Object o1 = arg1.eval(pojo, context);
            if (o1 == null) {
                throw new EvalException("Left argument is null");
            }
            if (!(o1 instanceof Number)) {
                throw new EvalException("Left argument should be number, but it is " + o1.getClass().getName());
            }
            Number n1 = (Number) o1;

            Object o2 = arg2.eval(pojo, context);
            if (o2 == null) {
                throw new EvalException("Left argument is null");
            }
            if (!(o2 instanceof Number)) {
                throw new EvalException("Left argument should be number, but it is " + o2.getClass().getName());
            }
            Number n2 = (Number) o2;

            switch (operator) {
                case "<"  : return n1.doubleValue() < n2.doubleValue();
                case ">"  : return n1.doubleValue() > n2.doubleValue();
                case ">=" : return n1.doubleValue() >= n2.doubleValue();
                case "<=" : return n1.doubleValue() <= n2.doubleValue();
            }

            throw new EvalException("Unexpected comparison type: " + operator);
        }
    }

    private class FieldExpr implements Expr<POJO> {
        private final String field;
        private final ExprResType exprResType;

        public FieldExpr(String field, ExprResType exprResType) {
            this.field = field;
            this.exprResType = exprResType;
        }

        public FieldExpr(String field) {
            this(field, ExprResType.UNKNOWN);
        }

        @Override
        public ExprResType resultType() {
            return exprResType;
        }

        @Override
        public Object eval(POJO pojo, EvaluationContext<POJO> context) {

            if (context != null && context.getFieldExtractorsMap() != null && context.getFieldExtractorsMap().containsKey(field)) {
                return context.getFieldExtractorsMap().get(field).apply(pojo);
            }
            if (context != null && context.getExternalFieldsExtractor() != null) {
                Opt<Object> o = context.getExternalFieldsExtractor().extractFieldValue(pojo, field);
                if (o.isDefined()) {
                    return o.get();
                }
            }
            if (fieldExtractors.containsKey(field)) {
                return fieldExtractors.get(field).apply(pojo).toString();
            }
            if (useReflection) {
                Opt<Object> v = ReflectionUtils.getFieldPathValue(pojo, field);
                if (v.isNotDefined()) {
                    throw new EvalException("Unable to get field '" + field + "' value via reflection");
                }
                return v.get();
            }

            throw new EvalException("Unable to evaluate field '" + field + "'");
        }
    }

    private class LiteralExpr implements Expr<POJO> {
        private final String literal;

        public LiteralExpr(String literal) {
            this.literal = literal;
        }

        @Override
        public ExprResType resultType() {
            return ExprResType.STR;
        }

        @Override
        public String eval(POJO pojo, EvaluationContext<POJO> context) {
            return literal;
        }
    }

    private class NumberExpr implements Expr<POJO> {
        private final double number;

        public NumberExpr(double number) {
            this.number = number;
        }

        @Override
        public ExprResType resultType() {
            return ExprResType.NUM;
        }

        @Override
        public Object eval(POJO pojo, EvaluationContext<POJO> context) {
            return number;
        }
    }

    public static <POJO> Builder<POJO> createForRule(String rule) {
        return new Builder<>(rule);
    }

    public static class Builder<POJO> implements Serializable {
        private String rule;
        private Class<POJO> pojoClass = null;
        private Map<String, Function<POJO,Object>> fieldExtractors = new HashMap<>();
        private boolean useReflection = true;

        public Builder(String rule) {
            this.rule = rule;
        }

        public Builder<POJO> validateAgainstClass(Class<POJO> cls) {
            this.pojoClass = cls;
            return this;
        }

        public Builder<POJO> withFieldExtractors(Map<String, Function<POJO,Object>> fieldExtractors) {
            this.fieldExtractors.putAll(fieldExtractors);
            return this;
        }

        public Builder<POJO> withFieldExtractor(String fieldName, Function<POJO,Object> fieldExtractor) {
            this.fieldExtractors.put(fieldName, fieldExtractor);
            return this;
        }

        public Builder<POJO> allowReflectionFieldLookup(boolean useReflection) {
            this.useReflection = useReflection;
            return this;
        }

        public Evaluator<POJO, Object> build() {
            return new Evaluator<>(rule, pojoClass, Object.class, fieldExtractors, useReflection);
        }

        public Evaluator<POJO, Boolean> buildBoolEvaluator() {
            return new Evaluator<>(rule, pojoClass, Boolean.class, fieldExtractors, useReflection);
        }

        public Evaluator<POJO, String> buildStringEvaluator() {
            return new Evaluator<>(rule, pojoClass, String.class, fieldExtractors, useReflection);
        }

        public Evaluator<POJO, Double> buildNumberEvaluator() {
            return new Evaluator<>(rule, pojoClass, Double.class, fieldExtractors, useReflection);
        }
    }

}
