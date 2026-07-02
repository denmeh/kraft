package com.github.denmeh.kraft.compiler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamplesCompileTest {
    private static final Path EXAMPLES_DIR = Path.of("examples");

    static Stream<Path> exampleFiles() throws IOException {
        try (Stream<Path> paths = Files.list(EXAMPLES_DIR)) {
            return paths
                    .filter(path -> path.toString().endsWith(".kraft"))
                    .sorted()
                    .toList()
                    .stream();
        }
    }

    @ParameterizedTest
    @MethodSource("exampleFiles")
    void compilesExampleFile(Path exampleFile) throws IOException {
        String source = Files.readString(exampleFile);
        Compiler.CompileResult result = new Compiler().compile(source);

        assertTrue(
                result.success(),
                () -> exampleFile.getFileName() + " failed: " + result.diagnostics()
        );
    }

    @Test
    void examplesDirectoryIsNotEmpty() throws IOException {
        try (Stream<Path> paths = Files.list(EXAMPLES_DIR)) {
            assertFalse(paths.filter(path -> path.toString().endsWith(".kraft")).findAny().isEmpty());
        }
    }
}
