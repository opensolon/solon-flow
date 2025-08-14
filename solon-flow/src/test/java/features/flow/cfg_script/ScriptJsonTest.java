package features.flow.cfg_script;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.Chain;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;

import java.util.concurrent.Executors;

/**
 * 手动配装风格
 *
 * @author noear 2025/1/11 created
 */
public class ScriptJsonTest {
    private static FlowEngine flowEngine = FlowEngine.newInstance();

    @BeforeAll
    public static void before() {
        flowEngine.register("case2FlowDriver", new Case2FlowDriver());
    }

    @Test
    public void case1_demo() throws Throwable {
        Chain chain = Chain.parseByUri("classpath:flow/script_case1.chain.json");

        flowEngine.eval(chain);
    }

    @Test
    public void case2_interrupt() throws Throwable {
        Chain chain = Chain.parseByUri("classpath:flow/script_case2.chain.json");

        FlowContext context = FlowContext.of();
        context.put("a", 2);
        context.put("b", 3);
        context.put("c", 4);

        //完整执行

        flowEngine.eval(chain, context);
        assert "n-3".equals(context.get("result"));
    }

    @Test
    public void case2_interrupt2() throws Throwable {
        Chain chain = Chain.parseByUri("classpath:flow/script_case2.chain.json");

        FlowContext context = FlowContext.of();
        context.put("a", 12);
        context.put("b", 13);
        context.put("c", 14);

        //执行一层
        flowEngine.eval(chain.getNode("n-2"), 1, context);
        assert context.get("result").equals(123);
    }

    @Test
    public void case3_exclusive() throws Throwable {
        Chain chain = Chain.parseByUri("classpath:flow/script_case3.chain.json");

        FlowContext context = FlowContext.of();
        context.put("day", 1);
        flowEngine.eval(chain, context);
        assert null == context.get("result");

        context = FlowContext.of();
        context.put("day", 3);
        flowEngine.eval(chain, context);
        assert context.get("result").equals(3);

        context = FlowContext.of();
        context.put("day", 7);
        flowEngine.eval(chain, context);
        assert context.get("result").equals(7);
    }

    @Test
    public void case4_inclusive() throws Throwable {
        Chain chain = Chain.parseByUri("classpath:flow/script_case4.chain.json");

        FlowContext context = FlowContext.of();
        context.put("day", 1);
        flowEngine.eval(chain, context);
        assert context.get("result").equals(0);

        context = FlowContext.of();
        context.put("day", 3);
        flowEngine.eval(chain, context);
        assert context.get("result").equals(3);
    }

    @Test
    public void case4_inclusive2() throws Throwable {
        Chain chain = Chain.parseByUri("classpath:flow/script_case4.chain.json");

        FlowContext context = FlowContext.of();
        context.put("day", 7);
        flowEngine.eval(chain, context);
        assert context.get("result").equals(10);
    }

    @Test
    public void case5_parallel() throws Throwable {
        Chain chain = Chain.parseByUri("classpath:flow/script_case5.chain.yml");

        FlowContext context = FlowContext.of();
        context.put("day", 7);
        flowEngine.eval(chain, context);
        assert context.get("result").equals(10);
    }

    @Test
    public void case8() throws Throwable {
        Chain chain = Chain.parseByUri("classpath:flow/script_case8.chain.yml");

        FlowContext context = FlowContext.of();
        context.put("result", 1);
        flowEngine.eval(chain, context);
        assert context.get("result").equals(3);
    }

    @Test
    public void case9_parallel_async() throws Throwable {
        Chain chain = Chain.parseByUri("classpath:flow/script_case9.chain.yml");

        FlowContext context = FlowContext.of();
        context.executor(Executors.newFixedThreadPool(4));

        flowEngine.eval(chain, context);
    }
}