package com.github.denmeh.kraft.compiler.lexer;

public enum TokenType {
    COMMAND,
    PERMISSION,
    TRIGGER,
    SEND,
    SET,
    TO,
    PLAYER,

    COMMAND_NAME,
    IDENTIFIER,
    STRING,
    NUMBER,
    VARIABLE,

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
