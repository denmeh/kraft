package com.github.denmeh.kraft.compiler.lexer;

public enum TokenType {
    COMMAND,
    PERMISSION,
    TRIGGER,
    SEND,
    TO,
    PLAYER,

    COMMAND_NAME,
    IDENTIFIER,
    STRING,
    NUMBER,

    PLUS,
    MINUS,
    STAR,
    SLASH,

    COLON,
    NEWLINE,
    INDENT,
    DEDENT,

    EOF
}
