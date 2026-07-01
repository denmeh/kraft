package com.github.denmeh.kraft.compiler.ast;

import java.util.List;
import java.util.Optional;

public record CommandDeclaration(
        SourceSpan span,
        String name,
        Optional<String> permission,
        Optional<TriggerBlock> trigger,
        List<Statement> misplacedStatements
) implements AstNode {
}
