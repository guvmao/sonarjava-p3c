package com.elvis.sonar.java.checks.comment;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.*;

import java.util.List;

/**
 * @author fengbingjian
 * @description 所有枚举类型字段都应该有Javadoc风格的注释
 * @since 2024/9/26 17:35
 **/
@Rule(key = "EnumConstantsMustHaveCommentRule")
public class EnumConstantsMustHaveCommentRule extends BaseTreeVisitor implements JavaFileScanner {

    private JavaFileScannerContext context;

    @Override
    public void scanFile(JavaFileScannerContext context) {
        this.context = context;
        scan(context.getTree());
    }

    @Override
    public void visitClass(ClassTree tree) {
        // 只检查枚举类型
        if (tree.is(Tree.Kind.ENUM)) {
            checkEnumConstants(tree);
        }
        super.visitClass(tree);
    }

    private void checkEnumConstants(ClassTree enumTree) {
        List<Tree> members = enumTree.members();
        boolean foundFirstConstant = false;
        boolean hasCommentBeforeFirstConstant = false;

        for (int i = 0; i < members.size(); i++) {
            Tree member = members.get(i);

            // 找到第一个枚举常量
            if (member.is(Tree.Kind.ENUM_CONSTANT)) {
                if (!foundFirstConstant) {
                    foundFirstConstant = true;
                    // 检查第一个枚举常量前是否有注释
                    hasCommentBeforeFirstConstant = hasCommentBefore(member, i > 0 ? members.get(i - 1) : null);

                    if (!hasCommentBeforeFirstConstant) {
                        context.reportIssue(this, enumTree.simpleName(),
                            String.format("枚举类【%s】的常量缺少Javadoc注释，所有枚举常量都应该有注释说明", enumTree.simpleName().name()));
                        break;
                    }
                }
            }
        }
    }

    private boolean hasCommentBefore(Tree tree, Tree previousTree) {
        // 检查树节点前是否有注释
        // 这里简化处理：检查枚举常量的第一个token前是否有注释
        SyntaxToken firstToken = tree.firstToken();
        if (firstToken == null) {
            return false;
        }

        // 获取token前的琐碎内容（包括注释）
        List<SyntaxTrivia> trivias = firstToken.trivias();
        for (SyntaxTrivia trivia : trivias) {
            String comment = trivia.comment();
            // 检查是否是Javadoc注释（以/**开头）
            if (comment.trim().startsWith("/**")) {
                return true;
            }
        }

        return false;
    }
}
