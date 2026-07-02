package com.github.denmeh.kraft.compiler.ast;

public record ComparisonExpression(
        SourceSpan span,
        ComparisonOperator operator,
        Expression left,
        Expression right
) implements Expression {
}
