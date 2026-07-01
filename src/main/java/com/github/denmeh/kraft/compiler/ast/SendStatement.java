package com.github.denmeh.kraft.compiler.ast;

public record SendStatement(SourceSpan span, String message) implements Statement {
}
