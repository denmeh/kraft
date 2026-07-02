package com.github.denmeh.kraft.compiler.semantics;

import com.github.denmeh.kraft.compiler.ast.BinaryExpression;
import com.github.denmeh.kraft.compiler.ast.BinaryOperator;
import com.github.denmeh.kraft.compiler.ast.CommandDeclaration;
import com.github.denmeh.kraft.compiler.ast.Expression;
import com.github.denmeh.kraft.compiler.ast.KraftFile;
import com.github.denmeh.kraft.compiler.ast.NumberLiteralExpression;
import com.github.denmeh.kraft.compiler.ast.SendStatement;
import com.github.denmeh.kraft.compiler.ast.Statement;
import com.github.denmeh.kraft.compiler.ast.TextLiteralExpression;
import com.github.denmeh.kraft.compiler.ast.TriggerBlock;
import com.github.denmeh.kraft.compiler.diagnostic.DiagnosticReporter;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class SemanticAnalyzer {
    public void analyze(KraftFile file, DiagnosticReporter reporter) {
        Set<String> commandNames = new HashSet<>();

        for (CommandDeclaration command : file.commands()) {
            if (!commandNames.add(command.name())) {
                reporter.error(
                        "Duplicate command '" + command.name() + "'",
                        command.span().line(),
                        command.span().column()
                );
            }

            validateTrigger(command, reporter);

            for (Statement statement : command.misplacedStatements()) {
                reporter.error(
                        "Statement is only allowed inside a trigger block",
                        statement.span().line(),
                        statement.span().column()
                );
            }
        }
    }

    private void validateTrigger(CommandDeclaration command, DiagnosticReporter reporter) {
        if (command.trigger().isEmpty()) {
            reporter.error(
                    "Missing trigger block",
                    command.span().line(),
                    command.span().column()
            );
            return;
        }

        TriggerBlock trigger = command.trigger().get();
        if (trigger.statements().isEmpty()) {
            reporter.error(
                    "Missing trigger block",
                    trigger.span().line(),
                    trigger.span().column()
            );
            return;
        }

        for (Statement statement : trigger.statements()) {
            validateStatement(statement, reporter);
        }
    }

    private void validateStatement(Statement statement, DiagnosticReporter reporter) {
        if (statement instanceof SendStatement send) {
            validateSend(send, reporter);
        }
    }

    private void validateSend(SendStatement send, DiagnosticReporter reporter) {
        Optional<KraftType> messageType = analyzeExpression(send.message(), reporter);
        if (messageType.isEmpty()) {
            return;
        }

        KraftType type = messageType.get();
        if (type != KraftType.NUMBER && type != KraftType.TEXT) {
            reporter.error(
                    "Send message must be a number or text",
                    send.message().span().line(),
                    send.message().span().column()
            );
        }
    }

    private Optional<KraftType> analyzeExpression(Expression expression, DiagnosticReporter reporter) {
        return switch (expression) {
            case NumberLiteralExpression ignored -> Optional.of(KraftType.NUMBER);
            case TextLiteralExpression ignored -> Optional.of(KraftType.TEXT);
            case BinaryExpression binary -> analyzeBinary(binary, reporter);
        };
    }

    private Optional<KraftType> analyzeBinary(BinaryExpression binary, DiagnosticReporter reporter) {
        Optional<KraftType> leftType = analyzeExpression(binary.left(), reporter);
        Optional<KraftType> rightType = analyzeExpression(binary.right(), reporter);

        if (leftType.isEmpty() || rightType.isEmpty()) {
            return Optional.empty();
        }

        KraftType left = leftType.get();
        KraftType right = rightType.get();

        return switch (binary.operator()) {
            case PLUS -> {
                if (left == KraftType.NUMBER && right == KraftType.NUMBER) {
                    yield Optional.of(KraftType.NUMBER);
                }
                if (left == KraftType.TEXT || right == KraftType.TEXT) {
                    yield Optional.of(KraftType.TEXT);
                }
                yield invalidBinary(binary, reporter, "Addition requires numbers or text");
            }
            case MINUS, MULTIPLY, DIVIDE -> {
                if (left == KraftType.NUMBER && right == KraftType.NUMBER) {
                    yield Optional.of(KraftType.NUMBER);
                }
                String operatorName = switch (binary.operator()) {
                    case MINUS -> "Subtraction";
                    case MULTIPLY -> "Multiplication";
                    case DIVIDE -> "Division";
                    case PLUS -> "Addition";
                };
                yield invalidBinary(binary, reporter, operatorName + " requires numbers");
            }
        };
    }

    private Optional<KraftType> invalidBinary(
            BinaryExpression binary,
            DiagnosticReporter reporter,
            String message
    ) {
        reporter.error(message, binary.span().line(), binary.span().column());
        return Optional.empty();
    }
}
