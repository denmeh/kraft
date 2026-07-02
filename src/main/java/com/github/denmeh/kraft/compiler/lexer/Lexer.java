package com.github.denmeh.kraft.compiler.lexer;

import com.github.denmeh.kraft.compiler.diagnostic.DiagnosticReporter;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public final class Lexer {
    private static final int INDENT_WIDTH = 4;

    private static final Map<String, TokenType> KEYWORDS = Map.of(
            "command", TokenType.COMMAND,
            "permission", TokenType.PERMISSION,
            "trigger", TokenType.TRIGGER,
            "send", TokenType.SEND,
            "to", TokenType.TO,
            "player", TokenType.PLAYER
    );

    private final String source;
    private final DiagnosticReporter reporter;
    private final List<Token> tokens = new ArrayList<>();
    private final Deque<Integer> indentStack = new ArrayDeque<>();

    private int index;
    private int line = 1;
    private int column = 1;

    public Lexer(String source, DiagnosticReporter reporter) {
        this.source = source;
        this.reporter = reporter;
        this.indentStack.push(0);
    }

    public List<Token> tokenize() {
        while (!isAtEnd()) {
            if (isLineStart()) {
                handleIndentation();
                if (isAtEnd() || isBlankLine()) {
                    continue;
                }
            }

            skipInlineWhitespace();
            if (isAtEnd()) {
                break;
            }

            scanToken();
        }

        while (indentStack.size() > 1) {
            indentStack.pop();
            tokens.add(token(TokenType.DEDENT, "", line, column));
        }

        tokens.add(token(TokenType.EOF, "", line, column));
        return List.copyOf(tokens);
    }

    private void handleIndentation() {
        int indent = 0;
        while (matchChar(' ')) {
            indent++;
        }

        if (peek() == '\r' || peek() == '\n') {
            return;
        }

        if (indent % INDENT_WIDTH != 0) {
            reporter.error("Indentation must use multiples of " + INDENT_WIDTH + " spaces", line, column);
        }

        int level = indent / INDENT_WIDTH;
        int current = indentStack.peek();

        if (level > current) {
            indentStack.push(level);
            tokens.add(token(TokenType.INDENT, "", line, column));
        } else if (level < current) {
            while (indentStack.peek() > level) {
                indentStack.pop();
                tokens.add(token(TokenType.DEDENT, "", line, column));
            }
            if (indentStack.peek() != level) {
                reporter.error("Inconsistent indentation", line, column);
            }
        }
    }

    private void scanToken() {
        char c = advance();

        switch (c) {
            case ':' -> tokens.add(token(TokenType.COLON, ":", line, column - 1));
            case '\r' -> {
                if (peek() == '\n') {
                    advance();
                }
                tokens.add(token(TokenType.NEWLINE, "\\n", line, column));
                line++;
                column = 1;
            }
            case '\n' -> {
                tokens.add(token(TokenType.NEWLINE, "\\n", line, column));
                line++;
                column = 1;
            }
            case '"' -> tokens.add(readString());
            case '+' -> tokens.add(token(TokenType.PLUS, "+", line, column - 1));
            case '-' -> tokens.add(token(TokenType.MINUS, "-", line, column - 1));
            case '*' -> tokens.add(token(TokenType.STAR, "*", line, column - 1));
            case '/' -> {
                if (isAlpha(peek())) {
                    tokens.add(readCommandName());
                } else {
                    tokens.add(token(TokenType.SLASH, "/", line, column - 1));
                }
            }
            default -> {
                if (isDigit(c)) {
                    tokens.add(readNumber(c));
                } else if (isAlpha(c)) {
                    tokens.add(readIdentifierOrKeyword());
                } else if (!isWhitespace(c)) {
                    reporter.error("Unexpected character '" + c + "'", line, column - 1);
                }
            }
        }
    }

    private Token readString() {
        int startLine = line;
        int startColumn = column;
        StringBuilder value = new StringBuilder();

        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\n' || peek() == '\r') {
                reporter.error("Unterminated string literal", startLine, startColumn);
                return token(TokenType.STRING, value.toString(), startLine, startColumn);
            }
            value.append(advance());
        }

        if (isAtEnd()) {
            reporter.error("Unterminated string literal", startLine, startColumn);
            return token(TokenType.STRING, value.toString(), startLine, startColumn);
        }

        advance();
        return token(TokenType.STRING, value.toString(), startLine, startColumn);
    }

    private Token readCommandName() {
        int startLine = line;
        int startColumn = column;
        StringBuilder name = new StringBuilder("/");

        while (isAlphaNumeric(peek())) {
            name.append(advance());
        }

        return token(TokenType.COMMAND_NAME, name.toString(), startLine, startColumn);
    }

    private Token readNumber(char first) {
        int startLine = line;
        int startColumn = column - 1;
        StringBuilder text = new StringBuilder();
        text.append(first);

        while (isDigit(peek())) {
            text.append(advance());
        }

        return token(TokenType.NUMBER, text.toString(), startLine, startColumn);
    }

    private Token readIdentifierOrKeyword() {
        int startLine = line;
        int startColumn = column - 1;
        StringBuilder text = new StringBuilder();
        text.append(source.charAt(index - 1));

        while (isIdentifierPart(peek())) {
            text.append(advance());
        }

        String lexeme = text.toString();
        TokenType type = KEYWORDS.getOrDefault(lexeme, TokenType.IDENTIFIER);
        return token(type, lexeme, startLine, startColumn);
    }

    private void skipInlineWhitespace() {
        while (!isAtEnd() && peek() == ' ') {
            advance();
        }
    }

    private boolean isBlankLine() {
        int savedIndex = index;
        int savedColumn = column;

        while (!isAtEnd() && peek() == ' ') {
            advance();
        }

        boolean blank = peek() == '\n' || peek() == '\r' || isAtEnd();
        if (peek() == '\r') {
            advance();
            if (peek() == '\n') {
                advance();
            }
            line++;
            column = 1;
        } else if (peek() == '\n') {
            advance();
            line++;
            column = 1;
        } else {
            index = savedIndex;
            column = savedColumn;
        }

        return blank;
    }

    private boolean isLineStart() {
        if (index == 0) {
            return true;
        }
        char previous = source.charAt(index - 1);
        return previous == '\n' || previous == '\r';
    }

    private Token token(TokenType type, String lexeme, int tokenLine, int tokenColumn) {
        return new Token(type, lexeme, tokenLine, tokenColumn);
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(index);
    }

    private char advance() {
        char c = source.charAt(index++);
        column++;
        return c;
    }

    private boolean matchChar(char expected) {
        if (isAtEnd() || source.charAt(index) != expected) {
            return false;
        }
        advance();
        return true;
    }

    private boolean isAtEnd() {
        return index >= source.length();
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentifierPart(char c) {
        return isAlphaNumeric(c) || c == '_' || c == '.';
    }
}
