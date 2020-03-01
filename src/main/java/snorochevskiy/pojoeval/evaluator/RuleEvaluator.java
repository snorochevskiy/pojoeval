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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class provides an engine for executing DSL expressions on POJOs.
 * @param <MSG>
 */
public class RuleEvaluator<MSG> implements Serializable {

    private final Expr<MSG,Boolean> compiled;
    private final Class<MSG> msgClass;
    private Map<String, Function<MSG,Object>> fieldExtractors = new HashMap<>();

    public RuleEvaluator(String rule) {
        this(rule, null, Collections.emptyMap());
    }

    public RuleEvaluator(String rule, Class<MSG> msgClass) {
        this(rule, msgClass, Collections.emptyMap());
    }

    public RuleEvaluator(String rule, Map<String, Function<MSG,Object>> fieldExtractors) {
        this(rule, null, fieldExtractors);

    }

    public RuleEvaluator(String rule, Class<MSG> msgClass, Map<String, Function<MSG,Object>> fieldExtractors) {
        this.msgClass = msgClass;
        this.fieldExtractors.putAll(fieldExtractors);

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

    public boolean evaluate(MSG msg) {
        return compiled.calc(msg);
    }

    Expr<MSG,Boolean> parseTopExpr(RuleDslParser.TopExprContext topExprContext) {
        return parseLogicExpr(topExprContext.logicExpr());
    }

    Expr<MSG,Boolean> parseLogicExpr(RuleDslParser.LogicExprContext logicExprContext) {
        return parseOrExpr(logicExprContext.orExpr());
    }

    Expr<MSG,Boolean> parseOrExpr(RuleDslParser.OrExprContext orExprContext) {
        if (orExprContext.getChildCount() == 1) {
            return parseAndExpr(orExprContext.andExpr());
        } else if (orExprContext.getChildCount() == 3){
            Expr<MSG,Boolean> e1 = parseOrExpr(orExprContext.orExpr());
            Expr<MSG,Boolean> e2 = parseAndExpr(orExprContext.andExpr());
            return new OrExpr(e1, e2);
        } else {
            throw new DslError("Unable to parse expression", orExprContext);
        }
    }

    Expr<MSG,Boolean> parseAndExpr(RuleDslParser.AndExprContext andExprContext) {
        if (andExprContext.getChildCount() == 1) {
            return parseNotExpr(andExprContext.notExpr());
        } else if (andExprContext.getChildCount() == 3) {
            Expr<MSG,Boolean> e1 = parseAndExpr(andExprContext.andExpr());
            Expr<MSG,Boolean> e2 = parseNotExpr(andExprContext.notExpr());
            return new AndExpr(e1, e2);
        } else {
            throw new DslError("Unable to parse expression", andExprContext);
        }
    }

    Expr<MSG,Boolean> parseNotExpr(RuleDslParser.NotExprContext notExprContext) {
        if (notExprContext.getChildCount() == 1) { // fallthrough
            return parseEqExpr(notExprContext.eqExpr());
        } if (notExprContext.getChildCount() == 2) { // NOT expr
            Expr<MSG,Boolean> e = parseNotExpr(notExprContext.notExpr());
            return new NotExpr(e);
        } if (notExprContext.getChildCount() == 3) { // ( expr )
            return parseLogicExpr(notExprContext.logicExpr());
        } else {
            throw new DslError("Unable to parse expression", notExprContext);
        }
    }

    @SuppressWarnings("unchecked")
    Expr<MSG,Boolean> parseEqExpr(RuleDslParser.EqExprContext eqExprContext) {
        if (eqExprContext.getChildCount() == 1) {
            throw new DslError("Unexpected token while parsing Eq expression", eqExprContext);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.Eq() != null) {
            Expr<MSG,String> e1 = parseTextExpr(eqExprContext.eqExpr());
            Expr<MSG,String> e2 = parseTextExpr(eqExprContext.relExpr());
            return new EqExpr(e1, e2);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.NEq() != null) {
            Expr<MSG,String> e1 = parseTextExpr(eqExprContext.eqExpr());
            Expr<MSG,String> e2 = parseTextExpr(eqExprContext.relExpr());
            return new NotEqExpr(e1, e2);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.StrContains() != null) {
            Expr<MSG,String> e1 = parseTextExpr(eqExprContext.eqExpr());
            Expr<MSG,String> e2 = parseTextExpr(eqExprContext.relExpr());
            return new StrContainsExpr(e1, e2);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.StrContainsRegexp() != null) {
            Expr<MSG,String> e = parseTextExpr(eqExprContext.eqExpr());
            String regex = parseText(eqExprContext.relExpr());
            return new ContainsRegexpExpr(e, regex);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.StrMatches() != null) {
            Expr<MSG,String> e = parseTextExpr(eqExprContext.eqExpr());
            String regex = parseText(eqExprContext.relExpr());
            return new MatchExpr(e, regex);
        } else if (eqExprContext.getChildCount() == 3 && eqExprContext.In() != null) {
            Expr<MSG,String> e = parseTextExpr(eqExprContext.relExpr());
            List<String> stringList = parseStringList(eqExprContext.stringList());
            return new InExpr(e, stringList);
        }
        throw new DslError("Unexpected expression", eqExprContext);
    }

    Expr<MSG,?> parseRelExpr(RuleDslParser.RelExprContext relExprContext) {
        if (relExprContext.getChildCount() == 1) {
            // Expecting only field or literal here
            return parseTextExpr(relExprContext);
        }
        throw new RuntimeException();
    }

    Expr<MSG,String> parseTextExpr(RuleDslParser.EqExprContext eqExprContext) {
        if (eqExprContext.getChildCount() == 1) {
            return parseTextExpr(eqExprContext.relExpr());
        } else {
            throw new RuntimeException();
        }
    }

    Expr<MSG,String> parseTextExpr(RuleDslParser.RelExprContext relExprContext) {
        if (relExprContext.getChildCount() == 1) {
            if (relExprContext.Identifier() != null) {
                String identifierName = relExprContext.Identifier().getText();
                if (msgClass == null
                        || fieldExtractors.containsKey(identifierName) || ReflectionUtils.hasField(msgClass, identifierName)) {
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

    private class OrExpr implements Expr<MSG, Boolean> {

        private final Expr<MSG,Boolean> e1;
        private final Expr<MSG,Boolean> e2;

        private OrExpr(Expr<MSG, Boolean> e1, Expr<MSG, Boolean> e2) {
            this.e1 = e1;
            this.e2 = e2;
        }

        @Override
        public Boolean calc(MSG msg) {
            Boolean v1 = e1.calc(msg);
            if (v1) {
                return true;
            }
            Boolean v2 = e2.calc(msg);
            return v2;
        }
    }

    private class AndExpr implements Expr<MSG, Boolean> {

        private final Expr<MSG,Boolean> e1;
        private final Expr<MSG,Boolean> e2;

        private AndExpr(Expr<MSG, Boolean> e1, Expr<MSG, Boolean> e2) {
            this.e1 = e1;
            this.e2 = e2;
        }

        @Override
        public Boolean calc(MSG msg) {
            Boolean v1 = e1.calc(msg);
            Boolean v2 = e2.calc(msg);
            return v1 && v2;
        }
    }

    private class NotExpr implements Expr<MSG, Boolean> {

        private final Expr<MSG,Boolean> e;

        private NotExpr(Expr<MSG, Boolean> e) {
            this.e = e;
        }

        @Override
        public Boolean calc(MSG msg) {
            Boolean v = e.calc(msg);
            return !v;
        }
    }

    private class EqExpr<T> implements Expr<MSG,Boolean> {
        private final Expr<MSG,T> arg1;
        private final Expr<MSG,T> arg2;

        public EqExpr(Expr<MSG,T> arg1, Expr<MSG,T> arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public Boolean calc(MSG msg) {
            T v1 = arg1.calc(msg);
            T v2 = arg2.calc(msg);

            if (v1 == null) {

            }

            return v1.equals(v2);
        }
    }

    private class NotEqExpr<T> implements Expr<MSG,Boolean> {
        private final Expr<MSG,T> arg1;
        private final Expr<MSG,T> arg2;

        public NotEqExpr(Expr<MSG,T> arg1, Expr<MSG,T> arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public Boolean calc(MSG msg) {
            T v1 = arg1.calc(msg);
            T v2 = arg2.calc(msg);
            return !v1.equals(v2);
        }
    }

    private class StrContainsExpr implements Expr<MSG, Boolean> {
        private final Expr<MSG,String> arg1;
        private final Expr<MSG,String> arg2;

        public StrContainsExpr(Expr<MSG,String> arg1, Expr<MSG,String> arg2) {
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public Boolean calc(MSG msg) {
            String s1 = arg1.calc(msg);
            String s2 = arg2.calc(msg);
            return s1.contains(s2);
        }
    }

    private class ContainsRegexpExpr implements Expr<MSG, Boolean> {
        private final Expr<MSG,String> arg1;
        private final String regexp;
        private final Pattern pattern;

        public ContainsRegexpExpr(Expr<MSG,String> arg1, String regexp) {
            this.arg1 = arg1;
            this.regexp = regexp;
            this.pattern = Pattern.compile(regexp);
        }

        @Override
        public Boolean calc(MSG msg) {
            String s = arg1.calc(msg);
            Matcher m = pattern.matcher(s);
            boolean result = m.find();
            return result;
        }
    }

    private class MatchExpr implements Expr<MSG, Boolean> {
        private final Expr<MSG,String> arg1;
        private final String regexp;
        private final Pattern pattern;

        public MatchExpr(Expr<MSG,String> arg1, String regexp) {
            this.arg1 = arg1;
            this.regexp = regexp;
            this.pattern = Pattern.compile(regexp);
        }

        @Override
        public Boolean calc(MSG msg) {
            String s = arg1.calc(msg);
            Matcher m = pattern.matcher(s);
            boolean result = m.matches();
            return result;
        }
    }

    private class InExpr implements Expr<MSG, Boolean> {
        private final Expr<MSG,String> fieldExpr;
        private final List<String> stringList;

        private InExpr(Expr<MSG, String> field, List<String> stringList) {
            this.fieldExpr = field;
            this.stringList = stringList;
        }

        @Override
        public Boolean calc(MSG msg) {
            String v = fieldExpr.calc(msg);
            return stringList.contains(v);
        }
    }

    private class FieldExpr implements Expr<MSG, String> {
        private final String field;

        public FieldExpr(String field) {
            this.field = field;
        }

        @Override
        public String calc(MSG msg) {
            String value = null;
            if (fieldExtractors.containsKey(field)) {
                value = fieldExtractors.get(field).apply(msg).toString();
            } else {
                value = ReflectionUtils.getFieldValueOrNull(msg, field);
            }

            if (value == null) {
                throw new EvalException("Unable to evaluate field '" + field + "'");
            }

            return value;
        }
    }

    private class LiteralExpr implements Expr<MSG, String> {
        private final String literal;

        public LiteralExpr(String literal) {
            this.literal = literal;
        }

        @Override
        public String calc(MSG msg) {
            return literal;
        }
    }

}
