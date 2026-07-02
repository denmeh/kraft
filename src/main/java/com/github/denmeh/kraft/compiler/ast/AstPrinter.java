package com.github.denmeh.kraft.compiler.ast;

import java.util.List;

public final class AstPrinter {
    private static final String INDENT = "  ";

    public String print(KraftFile file) {
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < file.commands().size(); i++) {
            if (i > 0) {
                output.append('\n');
            }
            printCommand(output, file.commands().get(i), 0);
        }

        return output.toString();
    }

    private void printCommand(StringBuilder output, CommandDeclaration command, int depth) {
        appendLine(output, depth, "Command(" + command.name() + ")");

        command.permission().ifPresent(permission ->
                appendLine(output, depth + 1, "Permission(" + permission + ")")
        );

        for (Statement statement : command.misplacedStatements()) {
            printStatement(output, statement, depth + 1);
        }

        command.trigger().ifPresent(trigger -> printTrigger(output, trigger, depth + 1));
    }

    private void printTrigger(StringBuilder output, TriggerBlock trigger, int depth) {
        appendLine(output, depth, "Trigger");

        printStatements(output, trigger.statements(), depth + 1);
    }

    private void printStatements(StringBuilder output, List<Statement> statements, int depth) {
        for (Statement statement : statements) {
            printStatement(output, statement, depth);
        }
    }

    private void printStatement(StringBuilder output, Statement statement, int depth) {
        switch (statement) {
            case SendStatement send ->
                    appendLine(output, depth, "Send(" + printExpression(send.message()) + ", player)");
            case SetStatement set ->
                    appendLine(output, depth, "Set({" + set.variableName() + "}, " + printExpression(set.value()) + ")");
            case IfStatement ifStatement -> printIf(output, ifStatement, depth);
        }
    }

    private void printIf(StringBuilder output, IfStatement ifStatement, int depth) {
        appendLine(output, depth, "If(" + printExpression(ifStatement.condition()) + ")");
        printStatements(output, ifStatement.body(), depth + 1);
    }

    private String printExpression(Expression expression) {
        return switch (expression) {
            case NumberLiteralExpression number -> number.value();
            case TextLiteralExpression text -> "\"" + text.value() + "\"";
            case VariableReferenceExpression variable -> "{" + variable.name() + "}";
            case BinaryExpression binary -> printBinary(binary);
            case ComparisonExpression comparison -> printComparison(comparison);
        };
    }

    private String printBinary(BinaryExpression binary) {
        String operator = switch (binary.operator()) {
            case PLUS -> "+";
            case MINUS -> "-";
            case MULTIPLY -> "*";
            case DIVIDE -> "/";
        };
        return "(" + printExpression(binary.left()) + " " + operator + " " + printExpression(binary.right()) + ")";
    }

    private String printComparison(ComparisonExpression comparison) {
        String operator = switch (comparison.operator()) {
            case EQUAL -> "is";
            case NOT_EQUAL -> "is not";
            case LESS_THAN -> "<";
            case GREATER_THAN -> ">";
            case LESS_THAN_OR_EQUAL -> "<=";
            case GREATER_THAN_OR_EQUAL -> ">=";
        };
        return "(" + printExpression(comparison.left()) + " " + operator + " " + printExpression(comparison.right()) + ")";
    }

    private static void appendLine(StringBuilder output, int depth, String line) {
        output.append(INDENT.repeat(depth));
        output.append(line);
        output.append('\n');
    }
}
