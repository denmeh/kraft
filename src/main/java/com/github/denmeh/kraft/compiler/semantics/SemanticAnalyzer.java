package com.github.denmeh.kraft.compiler.semantics;

import com.github.denmeh.kraft.compiler.ast.CommandDeclaration;
import com.github.denmeh.kraft.compiler.ast.KraftFile;
import com.github.denmeh.kraft.compiler.ast.Statement;
import com.github.denmeh.kraft.compiler.ast.TriggerBlock;
import com.github.denmeh.kraft.compiler.diagnostic.DiagnosticReporter;

import java.util.HashSet;
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
        }
    }
}
