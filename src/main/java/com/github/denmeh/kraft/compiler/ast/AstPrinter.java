package com.github.denmeh.kraft.compiler.ast;

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

        for (Statement statement : trigger.statements()) {
            printStatement(output, statement, depth + 1);
        }
    }

    private void printStatement(StringBuilder output, Statement statement, int depth) {
        switch (statement) {
            case SendStatement send ->
                    appendLine(output, depth, "Send(" + printExpression(send.message()) + ", player)");
            case SetStatement set ->
                    appendLine(output, depth, "Set({" + set.variableName() + "}, " + printExpression(set.value()) + ")");
        }
    }

    private String printExpression(Expression expression) {
        return switch (expression) {
            case NumberLiteralExpression number -> number.value();
            case TextLiteralExpression text -> "\"" + text.value() + "\"";
            case VariableReferenceExpression variable -> "{" + variable.name() + "}";
            case BinaryExpression binary -> printBinary(binary);
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

    private static void appendLine(StringBuilder output, int depth, String line) {
        output.append(INDENT.repeat(depth));
        output.append(line);
        output.append('\n');
    }
}
