package com.github.denmeh.kraft.compiler.parser;

import com.github.denmeh.kraft.compiler.KraftExamples;
import com.github.denmeh.kraft.compiler.ast.CommandDeclaration;
import com.github.denmeh.kraft.compiler.ast.KraftFile;
import com.github.denmeh.kraft.compiler.ast.SendStatement;
import com.github.denmeh.kraft.compiler.diagnostic.DiagnosticReporter;
import com.github.denmeh.kraft.compiler.lexer.Lexer;
import com.github.denmeh.kraft.compiler.lexer.Token;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserTest {
    @Test
    void parsesPingExample() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(KraftExamples.PING, reporter).tokenize();
        KraftFile file = new Parser(tokens, reporter).parse();

        assertFalse(reporter.hasErrors());
        assertEquals(1, file.commands().size());

        CommandDeclaration command = file.commands().getFirst();
        assertEquals("/ping", command.name());
        assertTrue(command.permission().isEmpty());
        assertEquals(1, command.trigger().orElseThrow().statements().size());

        SendStatement send = assertInstanceOf(
                SendStatement.class,
                command.trigger().orElseThrow().statements().getFirst()
        );
        assertEquals("Pong!", send.message());
    }

    @Test
    void parsesPingExampleWithPermission() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(KraftExamples.PING_WITH_PERMISSION, reporter).tokenize();
        KraftFile file = new Parser(tokens, reporter).parse();

        assertFalse(reporter.hasErrors());
        assertEquals(Optional.of("kraft.ping"), file.commands().getFirst().permission());
    }
}
