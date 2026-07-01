package com.github.denmeh.kraft.compiler.semantics;

import com.github.denmeh.kraft.compiler.KraftExamples;
import com.github.denmeh.kraft.compiler.diagnostic.Diagnostic;
import com.github.denmeh.kraft.compiler.diagnostic.DiagnosticReporter;
import com.github.denmeh.kraft.compiler.lexer.Lexer;
import com.github.denmeh.kraft.compiler.lexer.Token;
import com.github.denmeh.kraft.compiler.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticAnalyzerTest {
    private final SemanticAnalyzer analyzer = new SemanticAnalyzer();

    @Test
    void acceptsValidPingExample() {
        DiagnosticReporter reporter = analyze(KraftExamples.PING);
        assertFalse(reporter.hasErrors());
    }

    @Test
    void acceptsValidPingExampleWithPermission() {
        DiagnosticReporter reporter = analyze(KraftExamples.PING_WITH_PERMISSION);
        assertFalse(reporter.hasErrors());
    }

    @Test
    void reportsDuplicateCommands() {
        DiagnosticReporter reporter = analyze("""
                command /ping:
                    trigger:
                        send "One" to player
                command /ping:
                    trigger:
                        send "Two" to player
                """);

        assertTrue(reporter.hasErrors());
        assertTrue(hasMessage(reporter, "Duplicate command '/ping'"));
    }

    @Test
    void reportsMissingTriggerBlock() {
        DiagnosticReporter reporter = analyze("""
                command /ping:
                    permission: kraft.ping
                """);

        assertTrue(reporter.hasErrors());
        assertTrue(hasMessage(reporter, "Missing trigger block"));
    }

    @Test
    void reportsEmptyTriggerBlock() {
        DiagnosticReporter reporter = analyze("""
                command /ping:
                    trigger:
                """);

        assertTrue(reporter.hasErrors());
        assertTrue(hasMessage(reporter, "Missing trigger block"));
    }

    @Test
    void reportsStatementOutsideTriggerBlock() {
        DiagnosticReporter reporter = analyze("""
                command /ping:
                    send "Pong!" to player
                    trigger:
                        send "Ok" to player
                """);

        assertTrue(reporter.hasErrors());
        assertTrue(hasMessage(reporter, "Statement is only allowed inside a trigger block"));
    }

    private DiagnosticReporter analyze(String source) {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(source, reporter).tokenize();
        if (reporter.hasErrors()) {
            return reporter;
        }

        var ast = new Parser(tokens, reporter).parse();
        if (reporter.hasErrors()) {
            return reporter;
        }

        analyzer.analyze(ast, reporter);
        return reporter;
    }

    private static boolean hasMessage(DiagnosticReporter reporter, String message) {
        return reporter.diagnostics().stream()
                .map(Diagnostic::message)
                .anyMatch(text -> text.equals(message));
    }
}
