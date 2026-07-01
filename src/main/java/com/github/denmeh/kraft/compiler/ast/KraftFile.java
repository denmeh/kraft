package com.github.denmeh.kraft.compiler.ast;

import java.util.List;

public record KraftFile(List<CommandDeclaration> commands) implements AstNode {
}
