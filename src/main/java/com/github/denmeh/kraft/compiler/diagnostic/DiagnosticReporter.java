package com.github.denmeh.kraft.compiler.diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiagnosticReporter {
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    public void error(String message, int line, int column) {
        report(new Diagnostic(Diagnostic.Severity.ERROR, message, line, column));
    }

    public void warning(String message, int line, int column) {
        report(new Diagnostic(Diagnostic.Severity.WARNING, message, line, column));
    }

    public void report(Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

    public List<Diagnostic> diagnostics() {
        return Collections.unmodifiableList(diagnostics);
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity() == Diagnostic.Severity.ERROR);
    }
}
