package com.elvis.sonar.java.checks.concurrent;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.*;

import java.util.Arrays;
import java.util.List;

/**
 * @author fengbingjian
 * @description 创建线程或线程池时请指定有意义的线程名称，创建线程池请使用带ThreadFactory的构造函数，并实现ThreadFactory。
 * @since 2024/9/29 22:04
 **/
@Rule(key = "ThreadShouldSetNameRule")
public class ThreadShouldSetNameRule extends IssuableSubscriptionVisitor {

    private static final String THREAD = "Thread";
    private static final String THREAD_POOL_EXECUTOR = "ThreadPoolExecutor";
    private static final String SCHEDULED_THREAD_POOL_EXECUTOR = "ScheduledThreadPoolExecutor";

    private static final MethodMatchers EXECUTORS_METHODS = MethodMatchers.create()
        .ofTypes("java.util.concurrent.Executors")
        .names("newFixedThreadPool", "newSingleThreadExecutor", "newCachedThreadPool",
               "newScheduledThreadPool", "newSingleThreadScheduledExecutor")
        .withAnyParameters()
        .build();

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
    }

    @Override
    public void visitNode(Tree tree) {
        if (tree.is(Tree.Kind.NEW_CLASS)) {
            checkNewClass((NewClassTree) tree);
        } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
            checkMethodInvocation((MethodInvocationTree) tree);
        }
    }

    private void checkNewClass(NewClassTree newClassTree) {
        TypeTree identifier = newClassTree.identifier();
        if (identifier == null) {
            return;
        }

        String typeName = getTypeName(identifier);

        // 检查Thread实例化
        if (THREAD.equals(typeName)) {
            checkThreadCreation(newClassTree);
        }
        // 检查ThreadPoolExecutor实例化
        else if (THREAD_POOL_EXECUTOR.equals(typeName) || SCHEDULED_THREAD_POOL_EXECUTOR.equals(typeName)) {
            checkThreadPoolExecutorCreation(newClassTree);
        }
    }

    private void checkMethodInvocation(MethodInvocationTree methodInvocation) {
        // 检查Executors工具类的方法调用
        if (EXECUTORS_METHODS.matches(methodInvocation)) {
            Arguments arguments = methodInvocation.arguments();
            // 检查是否传入了ThreadFactory参数
            boolean hasThreadFactory = false;
            for (ExpressionTree arg : arguments) {
                if (isThreadFactoryType(arg)) {
                    hasThreadFactory = true;
                    break;
                }
            }

            if (!hasThreadFactory) {
                reportIssue(methodInvocation, "使用Executors创建线程池时，应该传入ThreadFactory参数以指定有意义的线程名称");
            }
        }
    }

    private void checkThreadCreation(NewClassTree newClassTree) {
        Arguments arguments = newClassTree.arguments();

        // 检查是否有setName调用或者构造函数中传入了name参数
        boolean hasName = false;

        // 检查构造函数参数中是否包含String类型的name参数
        for (ExpressionTree arg : arguments) {
            if (arg.is(Tree.Kind.STRING_LITERAL)) {
                hasName = true;
                break;
            }
        }

        if (!hasName) {
            reportIssue(newClassTree, "创建Thread时应该指定有意义的线程名称，建议使用Thread(Runnable, String)构造函数或调用setName()方法");
        }
    }

    private void checkThreadPoolExecutorCreation(NewClassTree newClassTree) {
        Arguments arguments = newClassTree.arguments();

        // ThreadPoolExecutor的构造函数如果包含ThreadFactory参数，则认为符合规范
        boolean hasThreadFactory = false;
        for (ExpressionTree arg : arguments) {
            if (isThreadFactoryType(arg)) {
                hasThreadFactory = true;
                break;
            }
        }

        if (!hasThreadFactory) {
            reportIssue(newClassTree, "创建ThreadPoolExecutor时应该使用带ThreadFactory参数的构造函数，以指定有意义的线程名称");
        }
    }

    private boolean isThreadFactoryType(ExpressionTree expression) {
        return expression.symbolType().is("java.util.concurrent.ThreadFactory");
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
}