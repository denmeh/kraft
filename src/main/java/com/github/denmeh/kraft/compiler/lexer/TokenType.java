package com.github.denmeh.kraft.compiler.lexer;

public enum TokenType {
    COMMAND,
    PERMISSION,
    TRIGGER,
    SEND,
    SET,
    IF,
    IS,
    NOT,
    GREATER,
    LESS,
    THAN,
    OR,
    EQUAL,
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

    LESS_THAN,
    GREATER_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN_OR_EQUAL,

    COLON,
    NEWLINE,
    INDENT,
    DEDENT,

    EOF
}
