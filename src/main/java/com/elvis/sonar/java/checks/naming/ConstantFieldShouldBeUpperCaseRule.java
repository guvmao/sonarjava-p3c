package com.elvis.sonar.java.checks.naming;

import com.elvis.sonar.java.checks.utils.VariableTreeCheckUtil;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * @author fengbingjian
 * @description 常量命名应全部大写并以下划线分隔
 * @since 2024/9/26 9:28
 **/
@Rule(key = "ConstantFieldShouldBeUpperCaseRule")
public class ConstantFieldShouldBeUpperCaseRule extends BaseTreeVisitor implements JavaFileScanner {

    private static final String FORMAT = "^[A-Z][A-Z0-9_]*$";
    private static final String DEFAULT_ALLOWED_EXCEPTIONS = "";

    @RuleProperty(
        key = "allowedExceptions",
        description = "允许例外的常量名，用逗号分隔（例如：serialVersionUID,log）",
        defaultValue = DEFAULT_ALLOWED_EXCEPTIONS
    )
    public String allowedExceptions = DEFAULT_ALLOWED_EXCEPTIONS;

    private Pattern pattern = null;
    private Set<String> allowedExceptionNames;
    private JavaFileScannerContext context;

    @Override
    public void scanFile(final JavaFileScannerContext context) {
        if (pattern == null) {
            pattern = Pattern.compile(FORMAT, Pattern.DOTALL);
        }
        allowedExceptionNames = Arrays.stream(allowedExceptions.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
        this.context = context;
        scan(context.getTree());
    }

    @Override
    public void visitClass(ClassTree tree) {
        if (tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.ENUM)) {
            for (Tree member : tree.members()) {
                if(!member.is(Tree.Kind.VARIABLE)){
                    continue;
                }
                VariableTree variableTree = (VariableTree) member;
                IdentifierTree simpleName = variableTree.simpleName();
                if (member.is(Tree.Kind.VARIABLE)
                        && VariableTreeCheckUtil.isStaticAndFinal(variableTree)
                        && !allowedExceptionNames.contains(simpleName.name())
                        && !pattern.matcher(simpleName.name()).matches()) {
                    context.reportIssue(this, simpleName, String.format("常量【%s】命名应全部大写并以下划线分隔", simpleName.name()));
                }
            }
        }
        super.visitClass(tree);
    }

}
