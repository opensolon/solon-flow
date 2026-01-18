package features.flow.manual.cfg_loop;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Inject;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.test.SolonTest;

import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@SolonTest
public class LoopTest {
    @Inject
    FlowEngine flowEngine;

    @Test
    public void case1() {
        FlowContext context = FlowContext.of();
        flowEngine.eval("fetch", context);

        context.remove("context");
        log.warn(new TreeMap<>(context.vars()).toString());

        assert 115 == context.vars().size();
    }

    @Test
    public void case2() {
        FlowContext context = FlowContext.of();
        flowEngine.eval("fetch2", context);

        context.remove("context");
        log.warn(new TreeMap<>(context.vars()).toString());

        assert context.<AtomicInteger>getAs("a").get() == 9;
        assert context.<AtomicInteger>getAs("b").get() == 3;
        assert context.<AtomicInteger>getAs("c").get() == 1;
    }

    @Test
    public void case_demo2() {
        FlowContext context = FlowContext.of();
        flowEngine.eval("loop_demo2", context);

        assert "c".equals(context.get("temp"));
    }

    @Test
    public void case_demo3() {
        FlowContext context = FlowContext.of();
        flowEngine.eval("loop_demo3", context);

        assert context.get("temp") instanceof Integer;
    }

    @Test
    public void case_demo4() {
        FlowContext context = FlowContext.of();
        flowEngine.eval("loop_demo4", context);

        assert context.get("temp") instanceof Integer;
        Assertions.assertEquals(8,context.get("temp"));
    }

    @Test
    public void case_demo5() {
        FlowContext context = FlowContext.of();
        flowEngine.eval("loop_demo5", context);

        assert context.get("temp") instanceof Integer;
        Assertions.assertEquals(2,context.get("temp"));
    }
}
