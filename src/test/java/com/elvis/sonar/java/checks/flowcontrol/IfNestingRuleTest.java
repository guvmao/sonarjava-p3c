package com.elvis.sonar.java.checks.flowcontrol;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

/**
 * 单元测试
 *
 * @author fengbingjian
 * @description if语句嵌套超过3层
 * @since 2026/8/29
 **/
public class IfNestingRuleTest {

    @Test
    void check() {
        CheckVerifier.newVerifier()
                .onFile("src/test/files/flowcontrol/IfNestingRule.java")
                .withCheck(new IfNestingRule())
                .verifyIssues();
    }
}
