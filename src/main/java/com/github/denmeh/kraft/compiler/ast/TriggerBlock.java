package com.github.denmeh.kraft.compiler.ast;

import java.util.List;

public record TriggerBlock(SourceSpan span, List<Statement> statements) implements AstNode {
}
