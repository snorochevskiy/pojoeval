package com.github.snorochevskiy.pojoeval.evaluator.exception;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class DslError extends RuntimeException {

    private String token;
    private int startPos;
    private int endPos;
    private int line;

    public DslError(String message, ParserRuleContext parserRuleContext) {
        this(message, parserRuleContext.getText(), parserRuleContext.getStart().getStartIndex(),
                parserRuleContext.getStart().getStopIndex(), parserRuleContext.getStart().getLine());
    }

    public DslError(String message, Token token) {
        this(message, token.getText(), token.getStartIndex(), token.getStopIndex(), token.getLine());
    }

    public DslError(String message, String token, int startPos, int endPos, int line) {
        super(message);
        this.token = token;
        this.startPos = startPos;
        this.endPos = endPos;
        this.line = line;
    }

    public String getToken() {
        return token;
    }

    public int getStartPos() {
        return startPos;
    }

    public int getEndPos() {
        return endPos;
    }

    public int getLine() {
        return line;
    }

    @Override
    public String toString() {
        return "DslError{" +
                "message='" + getMessage() + "'" +
                "token='" + token + '\'' +
                ", startPos=" + startPos +
                ", endPos=" + endPos +
                ", line=" + line +
                '}';
    }
}
