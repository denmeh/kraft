package com.github.denmeh.kraft.compiler.ast;

public record BinaryExpression(
        SourceSpan span,
        BinaryOperator operator,
        Expression left,
        Expression right
) implements Expression {
}
