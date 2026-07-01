package com.github.denmeh.kraft.compiler.lexer;

public record Token(TokenType type, String lexeme, int line, int column) {
}
