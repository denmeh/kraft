package com.github.denmeh.kraft.compiler;

public final class KraftExamples {
    public static final String PING = """
            command /ping:
                trigger:
                    send "Pong!" to player
            """;

    public static final String PING_WITH_PERMISSION = """
            command /ping:
                permission: kraft.ping
                trigger:
                    send "Pong!" to player
            """;

    public static final String MATH = """
            command /math:
                trigger:
                    send 5 + 5 to player
            """;

    public static final String TEXT_CONCAT = """
            command /greet:
                trigger:
                    send "Result: " + 10 to player
            """;

    private KraftExamples() {
    }
}
