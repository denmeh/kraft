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

    COLON,
    NEWLINE,
    INDENT,
    DEDENT,

    EOF
}
