package com.github.denmeh.kraft.compiler.ast;

public record TextLiteralExpression(SourceSpan span, String value) implements Expression {
}
