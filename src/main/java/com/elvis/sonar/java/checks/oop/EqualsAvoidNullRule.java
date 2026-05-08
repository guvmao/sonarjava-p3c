package com.elvis.sonar.java.checks.oop;

import com.elvis.sonar.java.checks.utils.MethodInvocationTreeCheckUtil;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * @author fengbingjian
 * @description Object的equals方法容易抛空指针异常，应使用常量或确定有值的对象来调用equals。
 * @since 2024/10/08 16:30
 */
@Rule(key = "EqualsAvoidNullRule")
public class EqualsAvoidNullRule extends IssuableSubscriptionVisitor {

    private static final String MESSAGE = "【%s】应该作为equals的参数，而不是调用方";
    private static final String METHOD_EQUALS = "equals";
    private static final String OBJECTS_CLASS = "java.util.Objects";
    private static final String DEFAULT_ALLOWED_CONSTANT_PATTERNS = "";

    @RuleProperty(
        key = "allowedConstantPatterns",
        description = "语义解析失败时允许放行的常量白名单，支持正则，多个表达式用逗号分隔，例如：StringConstants\\.STRING_ONE,.*Constants\\.STRING_.*",
        defaultValue = DEFAULT_ALLOWED_CONSTANT_PATTERNS
    )
    public String allowedConstantPatterns = DEFAULT_ALLOWED_CONSTANT_PATTERNS;

    private List<Pattern> compiledAllowedConstantPatterns = null;

    @Override
    public List<Tree.Kind> nodesToVisit() {
        // 我们想要访问方法调用表达式节点
        return Arrays.asList(Tree.Kind.METHOD_INVOCATION);
    }

    @Override
    public void visitNode(Tree tree) {
        if (compiledAllowedConstantPatterns == null) {
            compiledAllowedConstantPatterns = compileAllowedConstantPatterns();
        }
        if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
            MethodInvocationTree methodInvocation = (MethodInvocationTree) tree;
            if (METHOD_EQUALS.equals(MethodInvocationTreeCheckUtil.getMethodName(methodInvocation))) {
                checkEqualsInvocation(methodInvocation);
            }
        }
    }

    private void checkEqualsInvocation(MethodInvocationTree methodInvocation) {
        if (isObjectsEquals(methodInvocation)) {
            return;
        }

        ExpressionTree receiver = MethodInvocationTreeCheckUtil.getReceiver(methodInvocation);
        // 如果参数是字面量（如字符串字面量），则不需要进一步检查
        if (isLiteral(receiver)) {
            return;
        }

        // 检查参数是否为常量或确定不为null的对象
        if (!isConstantOrNonNullObject(receiver)) {
            String methodName = receiver == null ? "Object" : expressionToText(receiver);
            reportIssue(receiver, String.format(MESSAGE, methodName));
        }
    }

    private boolean isObjectsEquals(MethodInvocationTree methodInvocation) {
        if (!METHOD_EQUALS.equals(MethodInvocationTreeCheckUtil.getMethodName(methodInvocation))) {
            return false;
        }

        ExpressionTree receiver = MethodInvocationTreeCheckUtil.getReceiver(methodInvocation);
        if (receiver == null) {
            return false;
        }

        if (receiver.symbolType() != null && receiver.symbolType().is(OBJECTS_CLASS)) {
            return true;
        }

        return OBJECTS_CLASS.equals(expressionToText(receiver));
    }

    private boolean isLiteral(ExpressionTree expression) {
        return expression.is(Tree.Kind.STRING_LITERAL, Tree.Kind.CHAR_LITERAL, Tree.Kind.INT_LITERAL,
                Tree.Kind.LONG_LITERAL, Tree.Kind.FLOAT_LITERAL, Tree.Kind.DOUBLE_LITERAL, Tree.Kind.BOOLEAN_LITERAL);
    }

    private boolean isConstantOrNonNullObject(ExpressionTree expression) {
        // 字面量
        if (isLiteral(expression)) {
            return true;
        }

        // final局部变量或成员变量（直接标识符）
        if (expression.is(Tree.Kind.IDENTIFIER)) {
            IdentifierTree identifier = (IdentifierTree) expression;
            Symbol symbol = identifier.symbol();
            if (isFinalVariable(symbol)) {
                return true;
            }
        }

        if (expression.is(Tree.Kind.MEMBER_SELECT)) {
            MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) expression;
            Symbol symbol = memberSelect.identifier().symbol();
            if (isStaticFinalVariable(symbol)) {
                return true;
            }
            if (isFinalInstanceFieldViaThis(memberSelect)) {
                return true;
            }
            if ((symbol == null || symbol.isUnknown()) && matchesAllowedConstantPattern(memberSelect)) {
                return true;
            }
        }

        // 枚举类型不可能为null，递归追踪调用链根部是否为枚举
        // 覆盖：status.equals(x)、status.getCode().equals(x)、Status.ENABLE.getCode().equals(x)
        if (isRootedInEnum(expression)) {
            return true;
        }

        return false;
    }

    private boolean isEnumType(ExpressionTree expression) {
        Type type = expression.symbolType();
        if (type == null || type.isUnknown()) {
            return false;
        }
        Symbol.TypeSymbol typeSymbol = type.symbol();
        return typeSymbol != null && typeSymbol.isEnum();
    }

    private boolean isFinalInstanceFieldViaThis(MemberSelectExpressionTree memberSelect) {
        ExpressionTree qualifier = memberSelect.expression();
        if (!qualifier.is(Tree.Kind.IDENTIFIER)) {
            return false;
        }
        String qualifierName = ((IdentifierTree) qualifier).name();
        if (!"this".equals(qualifierName) && !"super".equals(qualifierName)) {
            return false;
        }
        return isFinalVariable(memberSelect.identifier().symbol());
    }

    /**
     * 递归追踪表达式链的根部，判断是否为枚举类型。
     * 覆盖：status.getCode().equals(x)、Status.ENABLE.getCode().equals(x) 等调用链场景。
     */
    private boolean isRootedInEnum(ExpressionTree expression) {
        if (expression == null) {
            return false;
        }
        if (isEnumType(expression)) {
            return true;
        }
        if (expression.is(Tree.Kind.MEMBER_SELECT)) {
            return isRootedInEnum(((MemberSelectExpressionTree) expression).expression());
        }
        if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
            return isRootedInEnum(((MethodInvocationTree) expression).methodSelect());
        }
        return false;
    }

    private boolean isFinalVariable(Symbol symbol) {
        if (symbol == null || !symbol.isVariableSymbol()) {
            return false;
        }
        if (symbol.isFinal()) {
            return true;
        }
        Tree declaration = symbol.declaration();
        return declaration != null
                && declaration.is(Tree.Kind.VARIABLE)
                && ((VariableTree) declaration).modifiers().modifiers().stream()
                .anyMatch(modifier -> modifier.modifier() == org.sonar.plugins.java.api.tree.Modifier.FINAL);
    }

    private boolean isStaticFinalVariable(Symbol symbol) {
        if (symbol == null || !symbol.isVariableSymbol()) {
            return false;
        }
        if (symbol.isFinal() && symbol.isStatic()) {
            return true;
        }
        Tree declaration = symbol.declaration();
        if (declaration == null || !declaration.is(Tree.Kind.VARIABLE)) {
            return false;
        }
        VariableTree variableTree = (VariableTree) declaration;
        boolean hasFinal = variableTree.modifiers().modifiers().stream()
                .anyMatch(modifier -> modifier.modifier() == org.sonar.plugins.java.api.tree.Modifier.FINAL);
        boolean hasStatic = variableTree.modifiers().modifiers().stream()
                .anyMatch(modifier -> modifier.modifier() == org.sonar.plugins.java.api.tree.Modifier.STATIC);
        return hasFinal && hasStatic;
    }

    private List<Pattern> compileAllowedConstantPatterns() {
        return Arrays.stream(allowedConstantPatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .map(Pattern::compile)
                .collect(Collectors.toList());
    }

    private boolean matchesAllowedConstantPattern(MemberSelectExpressionTree memberSelect) {
        String reference = expressionToText(memberSelect);
        return compiledAllowedConstantPatterns.stream()
                .anyMatch(pattern -> pattern.matcher(reference).matches());
    }

    private String expressionToText(ExpressionTree expression) {
        if (expression == null) {
            return "Object";
        }
        if (expression.is(Tree.Kind.IDENTIFIER)) {
            return ((IdentifierTree) expression).name();
        }
        if (expression.is(Tree.Kind.MEMBER_SELECT)) {
            MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) expression;
            return expressionToText(memberSelect.expression()) + "." + memberSelect.identifier().name();
        }
        if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
            MethodInvocationTree methodInvocation = (MethodInvocationTree) expression;
            String argumentsText = methodInvocation.arguments().stream()
                    .map(this::expressionToText)
                    .collect(Collectors.joining(", "));
            return expressionToText(methodInvocation.methodSelect()) + "(" + argumentsText + ")";
        }
        return expression.toString();
    }
}
