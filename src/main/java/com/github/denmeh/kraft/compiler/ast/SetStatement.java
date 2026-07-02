package com.github.denmeh.kraft.compiler.ast;

public record SetStatement(SourceSpan span, String variableName, Expression value) implements Statement {
}
