package com.elvis.sonar.java.checks.flowcontrol;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.List;

/**
 * @author fengbingjian
 * @description 在一个switch块内，每个case都要break/return来终止；且必须包含default语句。
 * @since 2024/9/29 22:04
 */
@Rule(key = "SwitchStatementRule")
public class SwitchStatementRule extends IssuableSubscriptionVisitor {

    private static final String DEFAULT_TEXT = "default";
    private static final String SWITCH_MUST_HAVE_DEFAULT_MESSAGE = "switch块缺少default语句";
    private static final String CASE_MUST_HAVE_BREAK_OR_RETURN_MESSAGE = "switch中每个case需要通过break/return等来终止";

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(Tree.Kind.SWITCH_STATEMENT);
    }

    @Override
    public void visitNode(Tree tree) {
        SwitchStatementTree switchStatement = (SwitchStatementTree) tree;
        checkDefault(switchStatement);
        checkFallThrough(switchStatement);
    }

    /**
     * 检查switch语句中是否包含default分支
     * @param switchStatement
     */
    private void checkDefault(SwitchStatementTree switchStatement) {
        boolean hasDefault = switchStatement.cases().stream().anyMatch(this::containsDefaultLabel);
        if (!hasDefault) {
            reportIssue(switchStatement, SWITCH_MUST_HAVE_DEFAULT_MESSAGE);
        }
    }

    /**
     * 判断case分组是否包含default标签
     *
     * @param caseGroup case分组
     * @return 是否包含default标签
     */
    private boolean containsDefaultLabel(CaseGroupTree caseGroup) {
        return caseGroup != null && caseGroup.labels() != null
                && caseGroup.labels().stream()
                .anyMatch(label -> DEFAULT_TEXT.equals(label.caseOrDefaultKeyword().text()));
    }

    /**
     * 检查switch语句中是否存在case分支的fall-through情况
     * @param switchStatement
     */
    private void checkFallThrough(SwitchStatementTree switchStatement) {
        for (CaseGroupTree caseGroup : switchStatement.cases()) {
            if (containsDefaultLabel(caseGroup)) {
                continue;
            }
            List<StatementTree> statements = caseGroup.body();
            if (statements.isEmpty() || !isTerminating(statements.get(statements.size() - 1))) {
                reportIssue(caseGroup, CASE_MUST_HAVE_BREAK_OR_RETURN_MESSAGE);
            }
        }
    }

    /**
     * 检查语句是否是终止语句
     * @param statement
     * @return
     */
    private boolean isTerminating(Tree statement) {
        if (statement == null) {
            return false;
        }
        if (statement.is(Tree.Kind.BLOCK)) {
            List<StatementTree> statements = ((BlockTree) statement).body();
            return !statements.isEmpty() && isTerminating(statements.get(statements.size() - 1));
        }
        if (statement.is(Tree.Kind.SWITCH_STATEMENT)) {
            return isSwitchTerminating((SwitchStatementTree) statement);
        }
        return statement.is(Tree.Kind.BREAK_STATEMENT, Tree.Kind.RETURN_STATEMENT, Tree.Kind.THROW_STATEMENT, Tree.Kind.CONTINUE_STATEMENT)
                || (statement.is(Tree.Kind.IF_STATEMENT) && isIfWithTerminatingThenOrElse((IfStatementTree) statement));
    }

    /**
     * 检查嵌套switch是否能够保证执行流离开当前外层语句。
     * 内层switch的break只会离开内层switch，不能作为外层case的终止语句。
     *
     * @param switchStatement 嵌套switch
     * @return 是否终止
     */
    private boolean isSwitchTerminating(SwitchStatementTree switchStatement) {
        return !switchStatement.cases().isEmpty()
                && switchStatement.cases().stream().anyMatch(this::containsDefaultLabel)
                && switchStatement.cases().stream().allMatch(caseGroup -> {
                    List<StatementTree> statements = caseGroup.body();
                    return !statements.isEmpty()
                            && isStrictlyTerminating(statements.get(statements.size() - 1));
                });
    }

    /**
     * 检查嵌套switch分支是否严格终止。这里不能把break当作终止语句，
     * 因为break只会跳出当前嵌套switch。
     *
     * @param statement 语句
     * @return 是否严格终止
     */
    private boolean isStrictlyTerminating(Tree statement) {
        if (statement == null) {
            return false;
        }
        if (statement.is(Tree.Kind.BLOCK)) {
            List<StatementTree> statements = ((BlockTree) statement).body();
            return !statements.isEmpty() && isStrictlyTerminating(statements.get(statements.size() - 1));
        }
        if (statement.is(Tree.Kind.SWITCH_STATEMENT)) {
            return isSwitchTerminating((SwitchStatementTree) statement);
        }
        if (statement.is(Tree.Kind.IF_STATEMENT)) {
            IfStatementTree ifStatement = (IfStatementTree) statement;
            return ifStatement.thenStatement() != null
                    && ifStatement.elseStatement() instanceof StatementTree
                    && isStrictlyTerminating(ifStatement.thenStatement())
                    && isStrictlyTerminating(ifStatement.elseStatement());
        }
        return statement.is(Tree.Kind.RETURN_STATEMENT, Tree.Kind.THROW_STATEMENT, Tree.Kind.CONTINUE_STATEMENT);
    }

    /**
     * 检查if语句是否包含终止的then或else分支
     * @param ifStatement
     * @return
     */
    private boolean isIfWithTerminatingThenOrElse(IfStatementTree ifStatement) {
        // 检查 then 分支是否终止
        boolean thenTerminates = ifStatement.thenStatement() != null && isTerminating(ifStatement.thenStatement());

        // 检查 else 分支是否终止
        boolean elseTerminates = (ifStatement.elseStatement() instanceof StatementTree)
                ? isTerminating(ifStatement.elseStatement())
                : true;  // 如果没有 else 分支，则认为它不违反规则

        return thenTerminates || elseTerminates;
    }

}