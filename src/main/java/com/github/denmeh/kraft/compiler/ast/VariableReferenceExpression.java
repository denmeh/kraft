package com.github.denmeh.kraft.compiler.ast;

public record VariableReferenceExpression(SourceSpan span, String name) implements Expression {
}
