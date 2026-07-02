package com.github.denmeh.kraft.compiler.semantics;

import com.github.denmeh.kraft.compiler.ast.BinaryExpression;
import com.github.denmeh.kraft.compiler.ast.BinaryOperator;
import com.github.denmeh.kraft.compiler.ast.CommandDeclaration;
import com.github.denmeh.kraft.compiler.ast.ComparisonExpression;
import com.github.denmeh.kraft.compiler.ast.ComparisonOperator;
import com.github.denmeh.kraft.compiler.ast.Expression;
import com.github.denmeh.kraft.compiler.ast.IfStatement;
import com.github.denmeh.kraft.compiler.ast.KraftFile;
import com.github.denmeh.kraft.compiler.ast.NumberLiteralExpression;
import com.github.denmeh.kraft.compiler.ast.SendStatement;
import com.github.denmeh.kraft.compiler.ast.SetStatement;
import com.github.denmeh.kraft.compiler.ast.Statement;
import com.github.denmeh.kraft.compiler.ast.TextLiteralExpression;
import com.github.denmeh.kraft.compiler.ast.TriggerBlock;
import com.github.denmeh.kraft.compiler.ast.VariableReferenceExpression;
import com.github.denmeh.kraft.compiler.diagnostic.DiagnosticReporter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

        Map<String, KraftType> scope = new HashMap<>();
        for (Statement statement : trigger.statements()) {
            validateStatement(statement, scope, reporter);
        }
    }

    private void validateStatement(
            Statement statement,
            Map<String, KraftType> scope,
            DiagnosticReporter reporter
    ) {
        switch (statement) {
            case SendStatement send -> validateSend(send, scope, reporter);
            case SetStatement set -> validateSet(set, scope, reporter);
            case IfStatement ifStatement -> validateIf(ifStatement, scope, reporter);
        }
    }

    private void validateIf(
            IfStatement ifStatement,
            Map<String, KraftType> scope,
            DiagnosticReporter reporter
    ) {
        Optional<KraftType> conditionType = analyzeExpression(ifStatement.condition(), scope, reporter);
        if (conditionType.isPresent() && conditionType.get() != KraftType.BOOLEAN) {
            reporter.error(
                    "If condition must be a boolean expression",
                    ifStatement.condition().span().line(),
                    ifStatement.condition().span().column()
            );
        }

        if (ifStatement.body().isEmpty()) {
            reporter.error(
                    "If block must contain at least one statement",
                    ifStatement.span().line(),
                    ifStatement.span().column()
            );
            return;
        }

        for (Statement statement : ifStatement.body()) {
            validateStatement(statement, scope, reporter);
        }
    }

    private void validateSend(
            SendStatement send,
            Map<String, KraftType> scope,
            DiagnosticReporter reporter
    ) {
        Optional<KraftType> messageType = analyzeExpression(send.message(), scope, reporter);
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

    private void validateSet(
            SetStatement set,
            Map<String, KraftType> scope,
            DiagnosticReporter reporter
    ) {
        Optional<KraftType> valueType = analyzeExpression(set.value(), scope, reporter);
        valueType.ifPresent(type -> scope.put(set.variableName(), type));
    }

    private Optional<KraftType> analyzeExpression(
            Expression expression,
            Map<String, KraftType> scope,
            DiagnosticReporter reporter
    ) {
        return switch (expression) {
            case NumberLiteralExpression ignored -> Optional.of(KraftType.NUMBER);
            case TextLiteralExpression ignored -> Optional.of(KraftType.TEXT);
            case VariableReferenceExpression variable -> analyzeVariableReference(variable, scope, reporter);
            case BinaryExpression binary -> analyzeBinary(binary, scope, reporter);
            case ComparisonExpression comparison -> analyzeComparison(comparison, scope, reporter);
        };
    }

    private Optional<KraftType> analyzeComparison(
            ComparisonExpression comparison,
            Map<String, KraftType> scope,
            DiagnosticReporter reporter
    ) {
        Optional<KraftType> leftType = analyzeExpression(comparison.left(), scope, reporter);
        Optional<KraftType> rightType = analyzeExpression(comparison.right(), scope, reporter);

        if (leftType.isEmpty() || rightType.isEmpty()) {
            return Optional.empty();
        }

        KraftType left = leftType.get();
        KraftType right = rightType.get();

        return switch (comparison.operator()) {
            case EQUAL, NOT_EQUAL -> {
                if (isEqualityCompatible(left, right)) {
                    yield Optional.of(KraftType.BOOLEAN);
                }
                yield invalidComparison(
                        comparison,
                        reporter,
                        "Equality comparisons require matching number or text types"
                );
            }
            case LESS_THAN, GREATER_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL -> {
                if (left == KraftType.NUMBER && right == KraftType.NUMBER) {
                    yield Optional.of(KraftType.BOOLEAN);
                }
                yield invalidComparison(
                        comparison,
                        reporter,
                        "Ordering comparisons require numbers"
                );
            }
        };
    }

    private static boolean isEqualityCompatible(KraftType left, KraftType right) {
        if (left == KraftType.BOOLEAN || right == KraftType.BOOLEAN) {
            return false;
        }
        return left == right;
    }

    private Optional<KraftType> invalidComparison(
            ComparisonExpression comparison,
            DiagnosticReporter reporter,
            String message
    ) {
        reporter.error(message, comparison.span().line(), comparison.span().column());
        return Optional.empty();
    }

    private Optional<KraftType> analyzeVariableReference(
            VariableReferenceExpression variable,
            Map<String, KraftType> scope,
            DiagnosticReporter reporter
    ) {
        KraftType type = scope.get(variable.name());
        if (type == null) {
            reporter.error(
                    "Undefined variable '{" + variable.name() + "}'",
                    variable.span().line(),
                    variable.span().column()
            );
            return Optional.empty();
        }
        return Optional.of(type);
    }

    private Optional<KraftType> analyzeBinary(
            BinaryExpression binary,
            Map<String, KraftType> scope,
            DiagnosticReporter reporter
    ) {
        Optional<KraftType> leftType = analyzeExpression(binary.left(), scope, reporter);
        Optional<KraftType> rightType = analyzeExpression(binary.right(), scope, reporter);

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
