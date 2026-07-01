package com.github.denmeh.kraft.compiler.codegen;

import com.github.denmeh.kraft.compiler.ast.KraftFile;

public final class BytecodeGenerator {
    public GeneratedClasses generate(KraftFile file) {
        throw new UnsupportedOperationException("Bytecode generation is not implemented yet");
    }

    public record GeneratedClasses(byte[] pluginMainClass, byte[] commandExecutorClass) {
    }
}
