package com.elvis.sonar.java.checks.concurrent;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

/**
 * 单元测试
 *
 * @author fengbingjian
 * @description 线程池应该手工创建，避免使用Executors自动创建
 * @since 2024/9/26 17:35
 **/
public class ThreadPoolCreationRuleTest {

    @Test
    void check() {
        CheckVerifier.newVerifier()
                .onFile("src/test/files/concurrent/ThreadPoolCreationRule.java")
                .withCheck(new ThreadPoolCreationRule())
                .verifyIssues();
    }
}