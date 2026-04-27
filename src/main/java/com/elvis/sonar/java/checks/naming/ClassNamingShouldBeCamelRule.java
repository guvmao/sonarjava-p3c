package com.elvis.sonar.java.checks.naming;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author fengbingjian
 * @description 类命名不符合UpperCamelCase命名风格
 * @since 2024/9/26 17:35
 **/
@Rule(key = "ClassNamingShouldBeCamelRule")
public class ClassNamingShouldBeCamelRule extends BaseTreeVisitor implements JavaFileScanner {

    private static final String DEFAULT_ABBREVIATIONS = "DO,DTO,VO,DAO,BO,DAOImpl,YunOS,AO,PO";

    @RuleProperty(
        key = "allowedAbbreviations",
        description = "允许的专有名词缩写，用逗号分隔（例如：DO,DTO,VO,DAO,BO）",
        defaultValue = DEFAULT_ABBREVIATIONS
    )
    public String allowedAbbreviations = DEFAULT_ABBREVIATIONS;

    private Pattern pattern = null;
    private JavaFileScannerContext context;

    @Override
    public void scanFile(JavaFileScannerContext context) {
        if (pattern == null) {
            String format = buildFormat();
            pattern = Pattern.compile(format, Pattern.DOTALL);
        }
        this.context = context;
        scan(context.getTree());
    }

    private String buildFormat() {
        String abbreviationPattern = buildAbbreviationPattern();
        String segmentPattern = abbreviationPattern.isEmpty()
            ? "[A-Z][a-z0-9]+"
            : "(?:" + abbreviationPattern + "|[A-Z][a-z0-9]+)";
        return "^I?(?:" + segmentPattern + ")+[A-Z]?$";
    }

    private String buildAbbreviationPattern() {
        if (allowedAbbreviations.trim().isEmpty()) {
            return "";
        }
        return Arrays.stream(allowedAbbreviations.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .sorted((left, right) -> Integer.compare(right.length(), left.length()))
            .map(Pattern::quote)
            .collect(Collectors.joining("|"));
    }

    @Override
    public void visitClass(ClassTree tree) {
        if (tree.is(Tree.Kind.CLASS) && tree.simpleName() != null && !pattern.matcher(tree.simpleName().name()).matches()) {
            context.reportIssue(this, tree.simpleName(), String.format("【%s】不符合UpperCamelCase命名风格", tree.simpleName().name()));
        }
        super.visitClass(tree);
    }
}