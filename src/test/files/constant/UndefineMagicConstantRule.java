package concurrent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UndefineMagicConstantRuleExample {
    public String check() {
        String key = "alibaba";
        if (key.equals("Id#taobao_1")) { // Noncompliant {{魔法值【"Id#taobao_1"】}}
            return "0";  // Noncompliant {{魔法值【"0"】}}
        }
        return key;
    }

    public String check2() {
        String key = "alibaba";
        String idtaobao = "Id#taobao_1";
        if (key.equals(idtaobao)) { // Compliant
            return idtaobao;  // Compliant
        }
        return key;
    }

    private static final Logger log = LoggerFactory.getLogger(UndefineMagicConstantRuleExample.class);

    public void lambdaWithLog(Consumer<Runnable> future) {
        if (true) {
            future.accept(() -> {
                log.info("start emqx auth plugin success"); // Compliant - log method in lambda
            });
        }
    }

    public void lambdaWithLogAndException(Consumer<Runnable> future) {
        if (true) {
            try {
                future.accept(() -> {
                    log.error("start emqx auth plugin failed"); // Compliant - log method in lambda
                });
            } catch (Exception e) {
                log.error("start emqx auth server failed", e); // Compliant - log method in lambda
            }
        }
    }
}