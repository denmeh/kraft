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

    public static final String VARIABLES = """
            command /math:
                trigger:
                    set {_answer} to 5 + 5
                    send {_answer} to player
            """;

    public static final String IF_EQUALITY = """
            command /check:
                trigger:
                    set {_x} to 24
                    if {_x} is 24:
                        send "match!" to player
            """;

    public static final String IF_COMPARISON = """
            command /compare:
                trigger:
                    if 2 < 3:
                        send "yes" to player
            """;

    public static final String IF_SKRIPT_COMPARISONS = """
            command /rank:
                trigger:
                    set {_score} to 15
                    if {_score} is greater than 10:
                        send "high" to player
                    if {_score} is not 0:
                        send "playing" to player
                    if {_score} is less than or equal to 20:
                        send "ok" to player
            """;

    private KraftExamples() {
    }
}
