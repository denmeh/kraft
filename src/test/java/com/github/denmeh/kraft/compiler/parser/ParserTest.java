package com.github.denmeh.kraft.compiler.parser;

import com.github.denmeh.kraft.compiler.KraftExamples;
import com.github.denmeh.kraft.compiler.ast.BinaryExpression;
import com.github.denmeh.kraft.compiler.ast.BinaryOperator;
import com.github.denmeh.kraft.compiler.ast.CommandDeclaration;
import com.github.denmeh.kraft.compiler.ast.ComparisonExpression;
import com.github.denmeh.kraft.compiler.ast.ComparisonOperator;
import com.github.denmeh.kraft.compiler.ast.IfStatement;
import com.github.denmeh.kraft.compiler.ast.KraftFile;
import com.github.denmeh.kraft.compiler.ast.NumberLiteralExpression;
import com.github.denmeh.kraft.compiler.ast.SendStatement;
import com.github.denmeh.kraft.compiler.ast.SetStatement;
import com.github.denmeh.kraft.compiler.ast.TextLiteralExpression;
import com.github.denmeh.kraft.compiler.ast.VariableReferenceExpression;
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
        TextLiteralExpression message = assertInstanceOf(TextLiteralExpression.class, send.message());
        assertEquals("Pong!", message.value());
    }

    @Test
    void parsesPingExampleWithPermission() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(KraftExamples.PING_WITH_PERMISSION, reporter).tokenize();
        KraftFile file = new Parser(tokens, reporter).parse();

        assertFalse(reporter.hasErrors());
        assertEquals(Optional.of("kraft.ping"), file.commands().getFirst().permission());
    }

    @Test
    void parsesMathExpressionInSend() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(KraftExamples.MATH, reporter).tokenize();
        KraftFile file = new Parser(tokens, reporter).parse();

        assertFalse(reporter.hasErrors());

        SendStatement send = assertInstanceOf(
                SendStatement.class,
                file.commands().getFirst().trigger().orElseThrow().statements().getFirst()
        );
        BinaryExpression expression = assertInstanceOf(BinaryExpression.class, send.message());
        assertEquals(BinaryOperator.PLUS, expression.operator());

        NumberLiteralExpression left = assertInstanceOf(NumberLiteralExpression.class, expression.left());
        NumberLiteralExpression right = assertInstanceOf(NumberLiteralExpression.class, expression.right());
        assertEquals("5", left.value());
        assertEquals("5", right.value());
    }

    @Test
    void parsesTextConcatenationInSend() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(KraftExamples.TEXT_CONCAT, reporter).tokenize();
        KraftFile file = new Parser(tokens, reporter).parse();

        assertFalse(reporter.hasErrors());

        SendStatement send = assertInstanceOf(
                SendStatement.class,
                file.commands().getFirst().trigger().orElseThrow().statements().getFirst()
        );
        BinaryExpression expression = assertInstanceOf(BinaryExpression.class, send.message());
        assertEquals(BinaryOperator.PLUS, expression.operator());
        assertInstanceOf(TextLiteralExpression.class, expression.left());
        assertInstanceOf(NumberLiteralExpression.class, expression.right());
    }

    @Test
    void respectsOperatorPrecedence() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        String source = """
                command /calc:
                    trigger:
                        send 2 + 3 * 4 to player
                """;
        List<Token> tokens = new Lexer(source, reporter).tokenize();
        KraftFile file = new Parser(tokens, reporter).parse();

        assertFalse(reporter.hasErrors());

        SendStatement send = assertInstanceOf(
                SendStatement.class,
                file.commands().getFirst().trigger().orElseThrow().statements().getFirst()
        );
        BinaryExpression addition = assertInstanceOf(BinaryExpression.class, send.message());
        assertEquals(BinaryOperator.PLUS, addition.operator());
        assertInstanceOf(NumberLiteralExpression.class, addition.left());

        BinaryExpression multiplication = assertInstanceOf(BinaryExpression.class, addition.right());
        assertEquals(BinaryOperator.MULTIPLY, multiplication.operator());
    }

    @Test
    void parsesSetStatementAndVariableReference() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(KraftExamples.VARIABLES, reporter).tokenize();
        KraftFile file = new Parser(tokens, reporter).parse();

        assertFalse(reporter.hasErrors());

        var statements = file.commands().getFirst().trigger().orElseThrow().statements();
        assertEquals(2, statements.size());

        SetStatement set = assertInstanceOf(SetStatement.class, statements.get(0));
        assertEquals("_answer", set.variableName());
        assertInstanceOf(BinaryExpression.class, set.value());

        SendStatement send = assertInstanceOf(SendStatement.class, statements.get(1));
        VariableReferenceExpression variable = assertInstanceOf(VariableReferenceExpression.class, send.message());
        assertEquals("_answer", variable.name());
    }

    @Test
    void parsesIfWithSkriptStyleEquality() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(KraftExamples.IF_EQUALITY, reporter).tokenize();
        KraftFile file = new Parser(tokens, reporter).parse();

        assertFalse(reporter.hasErrors());

        var statements = file.commands().getFirst().trigger().orElseThrow().statements();
        assertEquals(2, statements.size());

        IfStatement ifStatement = assertInstanceOf(IfStatement.class, statements.get(1));
        ComparisonExpression condition = assertInstanceOf(ComparisonExpression.class, ifStatement.condition());
        assertEquals(ComparisonOperator.EQUAL, condition.operator());
        assertInstanceOf(VariableReferenceExpression.class, condition.left());
        assertInstanceOf(NumberLiteralExpression.class, condition.right());
        assertEquals(1, ifStatement.body().size());
    }

    @Test
    void parsesIfWithSymbolComparison() {
        DiagnosticReporter reporter = new DiagnosticReporter();
        List<Token> tokens = new Lexer(KraftExamples.IF_COMPARISON, reporter).tokenize();
        KraftFile file = new Parser(tokens, reporter).parse();

        assertFalse(reporter.hasErrors());

        IfStatement ifStatement = assertInstanceOf(
                IfStatement.class,
                file.commands().getFirst().trigger().orElseThrow().statements().getFirst()
        );
        ComparisonExpression condition = assertInstanceOf(ComparisonExpression.class, ifStatement.condition());
        assertEquals(ComparisonOperator.LESS_THAN, condition.operator());
    }
}
