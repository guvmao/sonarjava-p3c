package com.elvis.sonar.java.checks.flowcontrol;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

/**
 * @author fengbingjian
 * @description if语句嵌套超过3层
 * @since 2026/8/29
 */
@Rule(key = "IfNestingRule")
public class IfNestingRule extends IssuableSubscriptionVisitor {

    private static final int MAX_NESTING_DEPTH = 3;
    private static final String MESSAGE = "if语句嵌套超过3层，请降低嵌套层数";

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Collections.singletonList(Tree.Kind.IF_STATEMENT);
    }

    @Override
    public void visitNode(Tree tree) {
        int depth = getNestingDepth(tree);
        if (depth == MAX_NESTING_DEPTH + 1) {
            reportIssue(tree, MESSAGE);
        }
    }

    private int getNestingDepth(Tree tree) {
        int depth = 1;
        Tree parent = tree.parent();
        while (parent != null && !isBoundary(parent)) {
            if (parent.is(Tree.Kind.IF_STATEMENT)) {
                depth++;
            }
            parent = parent.parent();
        }
        return depth;
    }

    private boolean isBoundary(Tree tree) {
        return tree.is(Tree.Kind.METHOD,
                Tree.Kind.LAMBDA_EXPRESSION,
                Tree.Kind.CLASS,
                Tree.Kind.INTERFACE,
                Tree.Kind.ENUM,
                Tree.Kind.ANNOTATION_TYPE);
    }
}
