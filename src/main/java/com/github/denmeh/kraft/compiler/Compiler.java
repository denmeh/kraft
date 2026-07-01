package com.github.denmeh.kraft.compiler;

import com.github.denmeh.kraft.compiler.ast.KraftFile;
import com.github.denmeh.kraft.compiler.diagnostic.Diagnostic;
import com.github.denmeh.kraft.compiler.diagnostic.DiagnosticReporter;
import com.github.denmeh.kraft.compiler.lexer.Lexer;
import com.github.denmeh.kraft.compiler.lexer.Token;
import com.github.denmeh.kraft.compiler.parser.Parser;
import com.github.denmeh.kraft.compiler.semantics.SemanticAnalyzer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Compiler {
    public CompileResult compile(String source) {
        DiagnosticReporter reporter = new DiagnosticReporter();

        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.tokenize();
        if (reporter.hasErrors()) {
            return CompileResult.failure(reporter.diagnostics());
        }

        Parser parser = new Parser(tokens, reporter);
        KraftFile ast = parser.parse();
        if (reporter.hasErrors()) {
            return CompileResult.failure(reporter.diagnostics());
        }

        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        semanticAnalyzer.analyze(ast, reporter);
        if (reporter.hasErrors()) {
            return CompileResult.failure(reporter.diagnostics());
        }

        return CompileResult.success(ast, reporter.diagnostics());
    }

    public record CompileResult(
            @Nullable KraftFile ast,
            List<Diagnostic> diagnostics,
            boolean success
    ) {
        public static CompileResult success(KraftFile ast, List<Diagnostic> diagnostics) {
            return new CompileResult(ast, diagnostics, true);
        }

        public static CompileResult failure(List<Diagnostic> diagnostics) {
            return new CompileResult(null, diagnostics, false);
        }
    }
}
