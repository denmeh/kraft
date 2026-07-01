package com.github.denmeh.kraft.compiler.ast;

public sealed interface Statement extends AstNode permits SendStatement {
    SourceSpan span();
}
