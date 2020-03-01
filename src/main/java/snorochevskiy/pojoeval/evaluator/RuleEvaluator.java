package snorochevskiy.pojoeval.evaluator;

import snorochevskiy.pojoeval.evaluator.exception.DslError;
import snorochevskiy.pojoeval.evaluator.exception.EvalException;
import snorochevskiy.pojoeval.reflect.ReflectionUtils;
import org.antlr.v4.runtime.*;

import snorochevskiy.pojoeval.rules.dsl.parser.RuleDslLexer;
import snorochevskiy.pojoeval.rules.dsl.parser.RuleDslParser;
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
public class RuleEvaluator<POJO> implements Serializable {

    private final Expr<POJO,Boolean> compiled;
    private final Class<POJO> msgClass;
    private Map<String, Function<POJO,Object>> fieldExtractors = new HashMap<>();
    private boolean useReflection;

    private RuleEvaluator(String rule, Class<POJO> msgClass, Map<String, Function<POJO,Object>> fieldExtractors, boolean useReflection) {
        this.msgClass = msgClass;
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
     * @param POJO
     * @return
     */
    public boolean evaluate(POJO POJO) {
        return compiled.calc(POJO);
    }

    Expr<POJO,Boolean> parseTopExpr(RuleDslParser.TopExprContext topExprContext) {
        return parseLogicExpr(topExprContext.logicExpr());
    }

    Expr<POJO,Boolean> parseLogicExpr(RuleDslParser.LogicExprContext logicExprContext) {
        return parseOrExpr(logicExprContext.orExpr());
    }

    Expr<POJO,Boolean> parseOrExpr(RuleDslParser.OrExprContext orExprContext) {
        if (orExprContext.getChildCount() == 1) {
            return parseAndExpr(orExprContext.andExpr());
        } else if (orExprContext.getChildCount() == 3){
            Expr<POJO,Boolean> e1 = parseOrExpr(orExprContext.orExpr());
            Expr<POJO,Boolean> e2 = parseAndExpr(orExprContext.andExpr());
            return new OrExpr(e1, e2);
        } else {
            throw new DslError("Unable to parse expression", orExprContext);
        }
    }

    Expr<POJO,Boolean> parseAndExpr(RuleDslParser.AndExprContext andExprContext) {
        if (andExprContext.getChildCount() == 1) {
            return parseNotExpr(andExprContext.notExpr());
        } else if (andExprContext.getChildCount() == 3) {
            Expr<POJO,Boolean> e1 = parseAndExpr(andExprContext.andExpr());
            Expr<POJO,Boolean> e2 = parseNotExpr(andExprContext.notExpr());
            return new AndExpr(e1, e2);
        } else {
            throw new DslError("Unable to parse expression", andExprContext);
        }
    }

    Expr<POJO,Boolean> parseNotExpr(RuleDslParser.NotExprContext notExprContext) {
        if (notExprContext.getChildCount() == 1) { // fallthrough
            return parseEqExpr(notExprContext.eqExpr());
        } if (notExprContext.getChildCount() == 2) { // NOT expr
            Expr<POJO,Boolean> e = parseNotExpr(notExprContext.notExpr());
            return new NotExpr(e);
        } if (notExprContext.getChildCount() == 3) { // ( expr )
            return parseLogicExpr(notExprContext.logicExpr());
        } else {
            throw new DslError("Unable to parse expression", notExprContext);
        }
    }

    @SuppressWarnings("unchecked")
    Expr<POJO,Boolean> parseEqExpr(RuleDslParser.EqExprContext eqExprContext) {
        if (eqExprContext.getChildCount() == 1) {
            throw new DslError("Unexpected token while parsing Eq expression", eqExprContext);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.Eq() != null) {
            Expr<POJO,String> e1 = parseTextExpr(eqExprContext.eqExpr());
            Expr<POJO,String> e2 = parseTextExpr(eqExprContext.relExpr());
            return new EqExpr(e1, e2);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.NEq() != null) {
            Expr<POJO,String> e1 = parseTextExpr(eqExprContext.eqExpr());
            Expr<POJO,String> e2 = parseTextExpr(eqExprContext.relExpr());
            return new NotEqExpr(e1, e2);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.StrContains() != null) {
            Expr<POJO,String> e1 = parseTextExpr(eqExprContext.eqExpr());
            Expr<POJO,String> e2 = parseTextExpr(eqExprContext.relExpr());
            return new StrContainsExpr(e1, e2);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.StrContainsRegexp() != null) {
            Expr<POJO,String> e = parseTextExpr(eqExprContext.eqExpr());
            String regex = parseText(eqExprContext.relExpr());
            return new ContainsRegexpExpr(e, regex);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.StrMatches() != null) {
            Expr<POJO,String> e = parseTextExpr(eqExprContext.eqExpr());
            String regex = parseText(eqExprContext.relExpr());
            return new MatchExpr(e, regex);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.In() != null) {
            Expr<POJO,String> e = parseTextExpr(eqExprContext.relExpr());
            List<String> stringList = parseStringList(eqExprContext.stringList());
            return new InExpr(e, stringList);
        }
        throw new DslError("Unexpected expression", eqExprContext);
    }

    Expr<POJO,?> parseRelExpr(RuleDslParser.RelExprContext relExprContext) {
        if (relExprContext.getChildCount() == 1) {
            // Expecting only field or literal here
            return parseTextExpr(relExprContext);
        }
        throw new RuntimeException();
    }

    Expr<POJO,String> parseTextExpr(RuleDslParser.EqExprContext eqExprContext) {
        if (eqExprContext.getChildCount() == 1) {
            return parseTextExpr(eqExprContext.relExpr());
        } else {
            throw new RuntimeException();
        }
    }

    Expr<POJO,String> parseTextExpr(RuleDslParser.RelExprContext relExprContext) {
        if (relExprContext.getChildCount() == 1) {
            if (relExprContext.Identifier() != null) {
                String identifierName = relExprContext.Identifier().getText();
                if (fieldExtractors.containsKey(identifierName)
                        || useReflection && msgClass == null
                        || useReflection && ReflectionUtils.hasField(msgClass, identifierName)) {
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

    String parseText(RuleDslParser.RelExprContext relExprContext) {
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

    private class OrExpr implements Expr<POJO, Boolean> {

        private final Expr<POJO,Boolean> e1;
        private final Expr<POJO,Boolean> e2;

        private OrExpr(Expr<POJO, Boolean> e1, Expr<POJO, Boolean> e2) {
            this.e1 = e1;
            this.e2 = e2;
        }

        @Override
        public Boolean calc(POJO POJO) {
            Boolean v1 = e1.calc(POJO);
            if (v1) {
                return true;
            }
            Boolean v2 = e2.calc(POJO);
            return v2;
        }
    }

    private class AndExpr implements Expr<POJO, Boolean> {

        private final Expr<POJO,Boolean> e1;
        private final Expr<POJO,Boolean> e2;

        private AndExpr(Expr<POJO, Boolean> e1, Expr<POJO, Boolean> e2) {
            this.e1 = e1;
            this.e2 = e2;
        }

        @Override
        public Boolean calc(POJO POJO) {
            Boolean v1 = e1.calc(POJO);
            Boolean v2 = e2.calc(POJO);
            return v1 && v2;
        }
    }

    private class NotExpr implements Expr<POJO, Boolean> {

        private final Expr<POJO,Boolean> e;

        private NotExpr(Expr<POJO, Boolean> e) {
            this.e = e;
        }

        @Override
        public Boolean calc(POJO POJO) {
            Boolean v = e.calc(POJO);
            return !v;
        }
    }

    private class EqExpr<T> implements Expr<POJO,Boolean> {
        private final Expr<POJO,T> arg1;
        private final Expr<POJO,T> arg2;

        public EqExpr(Expr<POJO,T> arg1, Expr<POJO,T> arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public Boolean calc(POJO POJO) {
            T v1 = arg1.calc(POJO);
            T v2 = arg2.calc(POJO);

            if (v1 == null) {

            }

            return v1.equals(v2);
        }
    }

    private class NotEqExpr<T> implements Expr<POJO,Boolean> {
        private final Expr<POJO,T> arg1;
        private final Expr<POJO,T> arg2;

        public NotEqExpr(Expr<POJO,T> arg1, Expr<POJO,T> arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public Boolean calc(POJO POJO) {
            T v1 = arg1.calc(POJO);
            T v2 = arg2.calc(POJO);
            return !v1.equals(v2);
        }
    }

    private class StrContainsExpr implements Expr<POJO, Boolean> {
        private final Expr<POJO,String> arg1;
        private final Expr<POJO,String> arg2;

        public StrContainsExpr(Expr<POJO,String> arg1, Expr<POJO,String> arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public Boolean calc(POJO POJO) {
            String s1 = arg1.calc(POJO);
            String s2 = arg2.calc(POJO);
            return s1.contains(s2);
        }
    }

    private class ContainsRegexpExpr implements Expr<POJO, Boolean> {
        private final Expr<POJO,String> arg1;
        private final String regexp;
        private final Pattern pattern;

        public ContainsRegexpExpr(Expr<POJO,String> arg1, String regexp) {
            this.arg1 = arg1;
            this.regexp = regexp;
            this.pattern = Pattern.compile(regexp);
        }

        @Override
        public Boolean calc(POJO POJO) {
            String s = arg1.calc(POJO);
            Matcher m = pattern.matcher(s);
            boolean result = m.find();
            return result;
        }
    }

    private class MatchExpr implements Expr<POJO, Boolean> {
        private final Expr<POJO,String> arg1;
        private final String regexp;
        private final Pattern pattern;

        public MatchExpr(Expr<POJO,String> arg1, String regexp) {
            this.arg1 = arg1;
            this.regexp = regexp;
            this.pattern = Pattern.compile(regexp);
        }

        @Override
        public Boolean calc(POJO POJO) {
            String s = arg1.calc(POJO);
            Matcher m = pattern.matcher(s);
            boolean result = m.matches();
            return result;
        }
    }

    private class InExpr implements Expr<POJO, Boolean> {
        private final Expr<POJO,String> fieldExpr;
        private final List<String> stringList;

        private InExpr(Expr<POJO, String> field, List<String> stringList) {
            this.fieldExpr = field;
            this.stringList = stringList;
        }

        @Override
        public Boolean calc(POJO POJO) {
            String v = fieldExpr.calc(POJO);
            return stringList.contains(v);
        }
    }

    private class FieldExpr implements Expr<POJO, String> {
        private final String field;

        public FieldExpr(String field) {
            this.field = field;
        }

        @Override
        public String calc(POJO POJO) {
            String value = null;
            if (fieldExtractors.containsKey(field)) {
                value = fieldExtractors.get(field).apply(POJO).toString();
            } else if (useReflection) {
                value = ReflectionUtils.getFieldValueOrNull(POJO, field);
            }

            if (value == null) {
                throw new EvalException("Unable to evaluate field '" + field + "'");
            }

            return value;
        }
    }

    private class LiteralExpr implements Expr<POJO, String> {
        private final String literal;

        public LiteralExpr(String literal) {
            this.literal = literal;
        }

        @Override
        public String calc(POJO POJO) {
            return literal;
        }
    }

    public static <POJO> Builder<POJO> createForRule(String rule) {
        return new Builder<POJO>(rule);
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

        public RuleEvaluator<POJO> build() {
            return new RuleEvaluator<>(rule, pojoClass, fieldExtractors, useReflection);
        }
    }
}
