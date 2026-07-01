package com.github.denmeh.kraft.compiler.diagnostic;

public record Diagnostic(Severity severity, String message, int line, int column) {
    public enum Severity {
        ERROR,
        WARNING
    }
}
