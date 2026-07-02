package com.github.denmeh.kraft.compiler.ast;

public record NumberLiteralExpression(SourceSpan span, String value) implements Expression {
}
