package com.github.denmeh.kraft.compiler.ast;

public record SendStatement(SourceSpan span, Expression message) implements Statement {
}
