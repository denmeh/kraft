package com.github.denmeh.kraft.compiler.ast;

import java.util.List;

public record IfStatement(SourceSpan span, Expression condition, List<Statement> body) implements Statement {
}
