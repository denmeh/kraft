package com.github.denmeh.kraft.compiler.packaging;

import com.github.denmeh.kraft.compiler.codegen.BytecodeGenerator;

import java.nio.file.Path;

public final class PluginPackager {
    public Path packagePlugin(BytecodeGenerator.GeneratedClasses classes, Path outputDirectory) {
        throw new UnsupportedOperationException("Plugin packaging is not implemented yet");
    }
}
