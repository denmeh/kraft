package com.github.denmeh.kraft.compiler.ast;

import com.github.denmeh.kraft.compiler.KraftExamples;
import com.github.denmeh.kraft.compiler.diagnostic.DiagnosticReporter;
import com.github.denmeh.kraft.compiler.lexer.Lexer;
import com.github.denmeh.kraft.compiler.lexer.Token;
import com.github.denmeh.kraft.compiler.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AstPrinterTest {
    private final AstPrinter printer = new AstPrinter();

    @Test
    void printsPingExample() {
        KraftFile file = parse(KraftExamples.PING);

        assertEquals(
                """
                Command(/ping)
                  Trigger
                    Send("Pong!", player)
                """.stripTrailing(),
                printer.print(file).stripTrailing()
        );
    }

    @Test
    void printsPingExampleWithPermission() {
        KraftFile file = parse(KraftExamples.PING_WITH_PERMISSION);

        assertEquals(
                """
                Command(/ping)
                  Permission(kraft.ping)
                  Trigger
                    Send("Pong!", player)
                """.stripTrailing(),
                printer.print(file).stripTrailing()
        );
    }

    @Test
    void printsMathExample() {
        KraftFile file = parse(KraftExamples.MATH);

        assertEquals(
                """
                Command(/math)
                  Trigger
                    Send((5 + 5), player)
                """.stripTrailing(),
                printer.print(file).stripTrailing()
        );
    }

    @Test
    void printsVariablesExample() {
        KraftFile file = parse(KraftExamples.VARIABLES);

        assertEquals(
                """
                Command(/math)
                  Trigger
                    Set({_answer}, (5 + 5))
                    Send({_answer}, player)
                """.stripTrailing(),
                printer.print(file).stripTrailing()
        );
    }

    @Test
    void printsIfEqualityExample() {
        KraftFile file = parse(KraftExamples.IF_EQUALITY);

        assertEquals(
                """
                Command(/check)
                  Trigger
                    Set({_x}, 24)
                    If(({_x} is 24))
                      Send("match!", player)
                """.stripTrailing(),
                printer.print(file).stripTrailing()
        );
    }

    private static KraftFile parse(String source) {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(source, reporter).tokenize();
        KraftFile file = new Parser(tokens, reporter).parse();
        assertFalse(reporter.hasErrors());
        return file;
    }
}
