package com.github.denmeh.kraft.compiler.parser;

import com.github.denmeh.kraft.compiler.ast.BinaryExpression;
import com.github.denmeh.kraft.compiler.ast.BinaryOperator;
import com.github.denmeh.kraft.compiler.ast.CommandDeclaration;
import com.github.denmeh.kraft.compiler.ast.ComparisonExpression;
import com.github.denmeh.kraft.compiler.ast.ComparisonOperator;
import com.github.denmeh.kraft.compiler.ast.Expression;
import com.github.denmeh.kraft.compiler.ast.IfStatement;
import com.github.denmeh.kraft.compiler.ast.KraftFile;
import com.github.denmeh.kraft.compiler.ast.NumberLiteralExpression;
import com.github.denmeh.kraft.compiler.ast.SendStatement;
import com.github.denmeh.kraft.compiler.ast.SetStatement;
import com.github.denmeh.kraft.compiler.ast.SourceSpan;
import com.github.denmeh.kraft.compiler.ast.Statement;
import com.github.denmeh.kraft.compiler.ast.TextLiteralExpression;
import com.github.denmeh.kraft.compiler.ast.TriggerBlock;
import com.github.denmeh.kraft.compiler.ast.VariableReferenceExpression;
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
            } else if (isStatementStart()) {
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
            statements.addAll(parseBlockStatements());
        }

        return new TriggerBlock(span, List.copyOf(statements));
    }

    private List<Statement> parseBlockStatements() {
        List<Statement> statements = new ArrayList<>();
        while (isStatementStart()) {
            statements.add(parseStatement());
        }
        return statements;
    }

    private boolean isStatementStart() {
        return check(TokenType.SEND) || check(TokenType.SET) || check(TokenType.IF);
    }

    private Statement parseStatement() {
        if (check(TokenType.SEND)) {
            return parseSendStatement();
        }
        if (check(TokenType.SET)) {
            return parseSetStatement();
        }
        if (check(TokenType.IF)) {
            return parseIfStatement();
        }

        Token token = peek();
        reporter.error("Expected statement", token.line(), token.column());
        advance();
        return new SendStatement(span(token), new NumberLiteralExpression(span(token), "0"));
    }

    private IfStatement parseIfStatement() {
        Token ifToken = consume(TokenType.IF, "Expected 'if'");
        Expression condition = parseCondition();
        consume(TokenType.COLON, "Expected ':' after if condition");
        consume(TokenType.NEWLINE, "Expected newline after if condition");

        List<Statement> body = new ArrayList<>();
        if (match(TokenType.INDENT)) {
            body = parseBlockStatements();
        }

        return new IfStatement(span(ifToken), condition, body);
    }

    private Expression parseCondition() {
        Expression left = parseExpression();

        if (match(TokenType.LESS_THAN)) {
            return new ComparisonExpression(
                    span(previous()),
                    ComparisonOperator.LESS_THAN,
                    left,
                    parseExpression()
            );
        }
        if (match(TokenType.GREATER_THAN)) {
            return new ComparisonExpression(
                    span(previous()),
                    ComparisonOperator.GREATER_THAN,
                    left,
                    parseExpression()
            );
        }
        if (match(TokenType.LESS_THAN_OR_EQUAL)) {
            return new ComparisonExpression(
                    span(previous()),
                    ComparisonOperator.LESS_THAN_OR_EQUAL,
                    left,
                    parseExpression()
            );
        }
        if (match(TokenType.GREATER_THAN_OR_EQUAL)) {
            return new ComparisonExpression(
                    span(previous()),
                    ComparisonOperator.GREATER_THAN_OR_EQUAL,
                    left,
                    parseExpression()
            );
        }
        if (match(TokenType.IS)) {
            return parseIsComparison(left, previous());
        }

        Token token = peek();
        reporter.error("Expected comparison operator", token.line(), token.column());
        return left;
    }

    private ComparisonExpression parseIsComparison(Expression left, Token isToken) {
        if (match(TokenType.NOT)) {
            return new ComparisonExpression(
                    span(isToken),
                    ComparisonOperator.NOT_EQUAL,
                    left,
                    parseExpression()
            );
        }
        if (match(TokenType.GREATER)) {
            consume(TokenType.THAN, "Expected 'than' after 'greater'");
            if (match(TokenType.OR)) {
                consume(TokenType.EQUAL, "Expected 'equal' after 'or'");
                consume(TokenType.TO, "Expected 'to' after 'equal'");
                return new ComparisonExpression(
                        span(isToken),
                        ComparisonOperator.GREATER_THAN_OR_EQUAL,
                        left,
                        parseExpression()
                );
            }
            return new ComparisonExpression(
                    span(isToken),
                    ComparisonOperator.GREATER_THAN,
                    left,
                    parseExpression()
            );
        }
        if (match(TokenType.LESS)) {
            consume(TokenType.THAN, "Expected 'than' after 'less'");
            if (match(TokenType.OR)) {
                consume(TokenType.EQUAL, "Expected 'equal' after 'or'");
                consume(TokenType.TO, "Expected 'to' after 'equal'");
                return new ComparisonExpression(
                        span(isToken),
                        ComparisonOperator.LESS_THAN_OR_EQUAL,
                        left,
                        parseExpression()
                );
            }
            return new ComparisonExpression(
                    span(isToken),
                    ComparisonOperator.LESS_THAN,
                    left,
                    parseExpression()
            );
        }
        return new ComparisonExpression(
                span(isToken),
                ComparisonOperator.EQUAL,
                left,
                parseExpression()
        );
    }

    private SendStatement parseSendStatement() {
        Token sendToken = consume(TokenType.SEND, "Expected 'send'");
        Expression message = parseExpression();
        consume(TokenType.TO, "Expected 'to' after message");
        consume(TokenType.PLAYER, "Expected 'player' after 'to'");

        if (check(TokenType.NEWLINE)) {
            advance();
        }

        return new SendStatement(span(sendToken), message);
    }

    private SetStatement parseSetStatement() {
        Token setToken = consume(TokenType.SET, "Expected 'set'");
        Token variableToken = consume(TokenType.VARIABLE, "Expected variable like {_x}");
        consume(TokenType.TO, "Expected 'to' after variable");
        Expression value = parseExpression();

        if (check(TokenType.NEWLINE)) {
            advance();
        }

        return new SetStatement(span(setToken), variableToken.lexeme(), value);
    }

    private Expression parseExpression() {
        return parseAddition();
    }

    private Expression parseAddition() {
        Expression left = parseMultiplication();

        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            BinaryOperator operator = previous().type() == TokenType.PLUS
                    ? BinaryOperator.PLUS
                    : BinaryOperator.MINUS;
            Expression right = parseMultiplication();
            left = new BinaryExpression(span(previous()), operator, left, right);
        }

        return left;
    }

    private Expression parseMultiplication() {
        Expression left = parsePrimary();

        while (match(TokenType.STAR) || match(TokenType.SLASH)) {
            BinaryOperator operator = previous().type() == TokenType.STAR
                    ? BinaryOperator.MULTIPLY
                    : BinaryOperator.DIVIDE;
            Expression right = parsePrimary();
            left = new BinaryExpression(span(previous()), operator, left, right);
        }

        return left;
    }

    private Expression parsePrimary() {
        if (match(TokenType.NUMBER)) {
            Token token = previous();
            return new NumberLiteralExpression(span(token), token.lexeme());
        }

        if (match(TokenType.STRING)) {
            Token token = previous();
            return new TextLiteralExpression(span(token), token.lexeme());
        }

        if (match(TokenType.VARIABLE)) {
            Token token = previous();
            return new VariableReferenceExpression(span(token), token.lexeme());
        }

        Token token = peek();
        reporter.error("Expected expression", token.line(), token.column());
        return new NumberLiteralExpression(span(token), "0");
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
