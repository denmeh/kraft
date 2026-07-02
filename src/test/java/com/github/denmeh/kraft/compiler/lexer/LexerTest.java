package com.github.denmeh.kraft.compiler.lexer;

import com.github.denmeh.kraft.compiler.KraftExamples;
import com.github.denmeh.kraft.compiler.diagnostic.DiagnosticReporter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LexerTest {
    @Test
    void tokenizesPingExample() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(KraftExamples.PING, reporter).tokenize();

        assertFalse(reporter.hasErrors());
        assertEquals(
                List.of(
                        TokenType.COMMAND,
                        TokenType.COMMAND_NAME,
                        TokenType.COLON,
                        TokenType.NEWLINE,
                        TokenType.INDENT,
                        TokenType.TRIGGER,
                        TokenType.COLON,
                        TokenType.NEWLINE,
                        TokenType.INDENT,
                        TokenType.SEND,
                        TokenType.STRING,
                        TokenType.TO,
                        TokenType.PLAYER,
                        TokenType.NEWLINE,
                        TokenType.DEDENT,
                        TokenType.DEDENT,
                        TokenType.EOF
                ),
                tokens.stream().map(Token::type).toList()
        );
        assertEquals("/ping", tokens.get(1).lexeme());
        assertEquals("Pong!", tokens.get(10).lexeme());
    }

    @Test
    void tokenizesPingExampleWithPermission() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(KraftExamples.PING_WITH_PERMISSION, reporter).tokenize();

        assertFalse(reporter.hasErrors());
        assertEquals(
                List.of(
                        TokenType.COMMAND,
                        TokenType.COMMAND_NAME,
                        TokenType.COLON,
                        TokenType.NEWLINE,
                        TokenType.INDENT,
                        TokenType.PERMISSION,
                        TokenType.COLON,
                        TokenType.IDENTIFIER,
                        TokenType.NEWLINE,
                        TokenType.TRIGGER,
                        TokenType.COLON,
                        TokenType.NEWLINE,
                        TokenType.INDENT,
                        TokenType.SEND,
                        TokenType.STRING,
                        TokenType.TO,
                        TokenType.PLAYER,
                        TokenType.NEWLINE,
                        TokenType.DEDENT,
                        TokenType.DEDENT,
                        TokenType.EOF
                ),
                tokens.stream().map(Token::type).toList()
        );
        assertEquals("kraft.ping", tokens.get(7).lexeme());
    }

    @Test
    void tokenizesMathExpression() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(KraftExamples.MATH, reporter).tokenize();

        assertFalse(reporter.hasErrors());
        assertEquals(
                List.of(
                        TokenType.COMMAND,
                        TokenType.COMMAND_NAME,
                        TokenType.COLON,
                        TokenType.NEWLINE,
                        TokenType.INDENT,
                        TokenType.TRIGGER,
                        TokenType.COLON,
                        TokenType.NEWLINE,
                        TokenType.INDENT,
                        TokenType.SEND,
                        TokenType.NUMBER,
                        TokenType.PLUS,
                        TokenType.NUMBER,
                        TokenType.TO,
                        TokenType.PLAYER,
                        TokenType.NEWLINE,
                        TokenType.DEDENT,
                        TokenType.DEDENT,
                        TokenType.EOF
                ),
                tokens.stream().map(Token::type).toList()
        );
        assertEquals("5", tokens.get(10).lexeme());
        assertEquals("5", tokens.get(12).lexeme());
    }

    @Test
    void tokenizesVariableSyntax() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(KraftExamples.VARIABLES, reporter).tokenize();

        assertFalse(reporter.hasErrors());
        assertEquals(TokenType.SET, tokens.get(9).type());
        assertEquals(TokenType.VARIABLE, tokens.get(10).type());
        assertEquals("_answer", tokens.get(10).lexeme());
        assertEquals(TokenType.TO, tokens.get(11).type());
        assertEquals(TokenType.VARIABLE, tokens.get(17).type());
        assertEquals("_answer", tokens.get(17).lexeme());
    }

    @Test
    void tokenizesIfCondition() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(KraftExamples.IF_COMPARISON, reporter).tokenize();

        assertFalse(reporter.hasErrors());
        assertEquals(TokenType.IF, tokens.get(9).type());
        assertEquals(TokenType.NUMBER, tokens.get(10).type());
        assertEquals(TokenType.LESS_THAN, tokens.get(11).type());
        assertEquals(TokenType.NUMBER, tokens.get(12).type());
    }
}
