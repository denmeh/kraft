package com.github.denmeh.kraft.compiler.parser;

import com.github.denmeh.kraft.compiler.ast.CommandDeclaration;
import com.github.denmeh.kraft.compiler.ast.KraftFile;
import com.github.denmeh.kraft.compiler.ast.SendStatement;
import com.github.denmeh.kraft.compiler.ast.SourceSpan;
import com.github.denmeh.kraft.compiler.ast.Statement;
import com.github.denmeh.kraft.compiler.ast.TriggerBlock;
import com.github.denmeh.kraft.compiler.diagnostic.DiagnosticReporter;
import com.github.denmeh.kraft.compiler.lexer.Token;
import com.github.denmeh.kraft.compiler.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Parser {
    private final List<Token> tokens;
    private final DiagnosticReporter reporter;
    private int current;

    public Parser(List<Token> tokens, DiagnosticReporter reporter) {
        this.tokens = tokens;
        this.reporter = reporter;
    }

    public KraftFile parse() {
        List<CommandDeclaration> commands = new ArrayList<>();

        while (!isAtEnd()) {
            while (match(TokenType.NEWLINE)) {
                // Skip blank lines between commands.
            }
            if (isAtEnd()) {
                break;
            }
            commands.add(parseCommandDeclaration());
        }

        return new KraftFile(List.copyOf(commands));
    }

    private CommandDeclaration parseCommandDeclaration() {
        Token commandToken = consume(TokenType.COMMAND, "Expected 'command'");
        consume(TokenType.COMMAND_NAME, "Expected command name like /ping");
        String commandName = previous().lexeme();
        consume(TokenType.COLON, "Expected ':' after command name");
        consume(TokenType.NEWLINE, "Expected newline after command declaration");

        SourceSpan span = span(commandToken);
        expectIndent(commandToken);

        Optional<String> permission = Optional.empty();
        Optional<TriggerBlock> trigger = Optional.empty();
        List<Statement> misplacedStatements = new ArrayList<>();

        while (!check(TokenType.DEDENT) && !isAtEnd()) {
            if (check(TokenType.PERMISSION)) {
                permission = Optional.of(parsePermission());
            } else if (check(TokenType.TRIGGER)) {
                trigger = Optional.of(parseTriggerBlock());
            } else if (check(TokenType.SEND)) {
                misplacedStatements.add(parseStatement());
            } else {
                Token token = peek();
                reporter.error("Unexpected token in command body", token.line(), token.column());
                advance();
            }
        }

        closeBlock();

        return new CommandDeclaration(
                span,
                commandName,
                permission,
                trigger,
                List.copyOf(misplacedStatements)
        );
    }

    private String parsePermission() {
        consume(TokenType.PERMISSION, "Expected 'permission'");
        consume(TokenType.COLON, "Expected ':' after 'permission'");
        Token permissionToken = consume(TokenType.IDENTIFIER, "Expected permission value");
        consume(TokenType.NEWLINE, "Expected newline after permission");
        return permissionToken.lexeme();
    }

    private TriggerBlock parseTriggerBlock() {
        Token triggerToken = consume(TokenType.TRIGGER, "Expected 'trigger'");
        consume(TokenType.COLON, "Expected ':' after 'trigger'");
        consume(TokenType.NEWLINE, "Expected newline after trigger declaration");

        SourceSpan span = span(triggerToken);
        List<Statement> statements = new ArrayList<>();

        if (match(TokenType.INDENT)) {
            while (check(TokenType.SEND)) {
                statements.add(parseStatement());
            }
        }

        return new TriggerBlock(span, List.copyOf(statements));
    }

    private Statement parseStatement() {
        Token sendToken = consume(TokenType.SEND, "Expected 'send'");
        Token messageToken = consume(TokenType.STRING, "Expected string message after 'send'");
        consume(TokenType.TO, "Expected 'to' after message");
        consume(TokenType.PLAYER, "Expected 'player' after 'to'");

        if (check(TokenType.NEWLINE)) {
            advance();
        }

        return new SendStatement(span(sendToken), messageToken.lexeme());
    }

    private void closeBlock() {
        while (match(TokenType.NEWLINE)) {
            // Skip blank lines before closing dedents.
        }
        while (match(TokenType.DEDENT)) {
            // Close remaining indentation levels for this command.
        }
    }

    private void expectIndent(Token context) {
        if (!match(TokenType.INDENT)) {
            reporter.error("Expected indented block", context.line(), context.column());
        }
    }

    private SourceSpan span(Token token) {
        return new SourceSpan(token.line(), token.column());
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type() == type;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }

        Token token = peek();
        reporter.error(message, token.line(), token.column());
        if (!isAtEnd()) {
            advance();
        }
        return token;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
