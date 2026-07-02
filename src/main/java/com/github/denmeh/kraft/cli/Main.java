package com.github.denmeh.kraft.cli;

import com.github.denmeh.kraft.compiler.Compiler;
import com.github.denmeh.kraft.compiler.ast.AstPrinter;
import com.github.denmeh.kraft.compiler.diagnostic.Diagnostic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: kraft <file.kraft>");
            System.exit(1);
        }

        Path sourcePath = Path.of(args[0]);
        try {
            String source = Files.readString(sourcePath);
            Compiler compiler = new Compiler();
            Compiler.CompileResult result = compiler.compile(source);

            for (Diagnostic diagnostic : result.diagnostics()) {
                System.err.printf(
                        "%s at %d:%d: %s%n",
                        diagnostic.severity(),
                        diagnostic.line(),
                        diagnostic.column(),
                        diagnostic.message()
                );
            }

            if (!result.success()) {
                System.exit(1);
            }

            System.out.println("Parsed " + result.ast().commands().size() + " command(s)");
            System.out.println(new AstPrinter().print(result.ast()).stripTrailing());
        } catch (IOException e) {
            System.err.println("Failed to read " + sourcePath + ": " + e.getMessage());
            System.exit(1);
        }
    }
}
