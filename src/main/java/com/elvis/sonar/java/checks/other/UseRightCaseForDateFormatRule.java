package com.elvis.sonar.java.checks.other;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.*;

import java.util.Arrays;
import java.util.List;

/**
 * @author fengbingjian
 * @description 日期格式化时，年份应该使用小写"y"而不是大写"Y"
 * 说明：大写'Y'表示week year（周年），小写'y'表示calendar year（日历年）
 * 如果一周跨越两年，使用YYYY可能导致年份计算错误
 * @since 2024/9/29 22:04
 **/
@Rule(key = "UseRightCaseForDateFormatRule")
public class UseRightCaseForDateFormatRule extends IssuableSubscriptionVisitor {

    private static final String SIMPLE_DATE_FORMAT = "SimpleDateFormat";
    private static final String LOW_CASE_4Y = "yyyy";
    private static final String LOW_CASE_2Y = "yy";

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(Tree.Kind.NEW_CLASS);
    }

    @Override
    public void visitNode(Tree tree) {
        NewClassTree newClassTree = (NewClassTree) tree;

        // 检查是否是SimpleDateFormat的实例化
        if (!isSimpleDateFormat(newClassTree)) {
            return;
        }

        // 获取构造函数参数
        Arguments arguments = newClassTree.arguments();
        if (arguments.isEmpty()) {
            return;
        }

        // 检查第一个参数（日期格式字符串）
        ExpressionTree firstArg = arguments.get(0);
        if (firstArg.is(Tree.Kind.STRING_LITERAL)) {
            LiteralTree literal = (LiteralTree) firstArg;
            String formatPattern = literal.value();
            checkDatePattern(formatPattern, literal);
        }
    }

    private boolean isSimpleDateFormat(NewClassTree newClassTree) {
        TypeTree identifier = newClassTree.identifier();
        if (identifier == null) {
            return false;
        }

        String typeName = getTypeName(identifier);
        return SIMPLE_DATE_FORMAT.equals(typeName);
    }

    private String getTypeName(TypeTree typeTree) {
        if (typeTree.is(Tree.Kind.IDENTIFIER)) {
            return ((IdentifierTree) typeTree).name();
        } else if (typeTree.is(Tree.Kind.MEMBER_SELECT)) {
            MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) typeTree;
            return memberSelect.identifier().name();
        }
        return "";
    }

    private void checkDatePattern(String pattern, LiteralTree literal) {
        // 移除引号
        String cleanPattern = pattern.replace("\"", "");
        String lowerCasePattern = cleanPattern.toLowerCase();

        // 检查是否使用了错误的大写Y
        if (!cleanPattern.startsWith(LOW_CASE_4Y) && lowerCasePattern.startsWith(LOW_CASE_4Y)) {
            reportIssue(literal, String.format("日期格式化【%s】应该使用小写'yyyy'而不是大写'YYYY'，大写'Y'表示week year可能导致跨年周计算错误", cleanPattern));
        } else if (!cleanPattern.startsWith(LOW_CASE_2Y) && lowerCasePattern.startsWith(LOW_CASE_2Y)) {
            reportIssue(literal, String.format("日期格式化【%s】应该使用小写'yy'而不是大写'YY'，大写'Y'表示week year可能导致跨年周计算错误", cleanPattern));
        }
    }
}