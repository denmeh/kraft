package com.github.denmeh.kraft.compiler.ast;

public sealed interface Expression extends AstNode
        permits NumberLiteralExpression, TextLiteralExpression, BinaryExpression,
                VariableReferenceExpression, ComparisonExpression {
    SourceSpan span();
}
