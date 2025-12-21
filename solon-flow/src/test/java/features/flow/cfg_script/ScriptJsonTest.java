package features.flow.cfg_script;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.Graph;
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
        Graph graph = Graph.fromUri("classpath:flow/script_case1.graph.json");

        flowEngine.eval(graph);
    }

    @Test
    public void case2_interrupt() throws Throwable {
        Graph graph = Graph.fromUri("classpath:flow/script_case2.graph.json");

        FlowContext context = FlowContext.of();
        context.put("a", 2);
        context.put("b", 3);
        context.put("c", 4);

        //完整执行

        flowEngine.eval(graph, context);
        assert "n-3".equals(context.getAs("result"));
    }

    @Test
    public void case2_interrupt2() throws Throwable {
        Graph graph = Graph.fromUri("classpath:flow/script_case2.graph.json");

        FlowContext context = FlowContext.of();
        context.put("a", 12);
        context.put("b", 13);
        context.put("c", 14);

        //执行一层
        flowEngine.eval(graph, "n-2", 1, context);
        assert context.getAs("result").equals(123);
    }

    @Test
    public void case3_exclusive() throws Throwable {
        Graph graph = Graph.fromUri("classpath:flow/script_case3.graph.json");

        FlowContext context = FlowContext.of();
        context.put("day", 1);
        flowEngine.eval(graph, context);
        assert null == context.getAs("result");

        context = FlowContext.of();
        context.put("day", 3);
        flowEngine.eval(graph, context);
        assert context.getAs("result").equals(3);

        context = FlowContext.of();
        context.put("day", 7);
        flowEngine.eval(graph, context);
        assert context.getAs("result").equals(7);
    }

    @Test
    public void case4_inclusive() throws Throwable {
        Graph graph = Graph.fromUri("classpath:flow/script_case4.graph.json");

        FlowContext context = FlowContext.of();
        context.put("day", 1);
        flowEngine.eval(graph, context);
        assert context.getAs("result").equals(0);

        context = FlowContext.of();
        context.put("day", 3);
        flowEngine.eval(graph, context);
        assert context.getAs("result").equals(3);
    }

    @Test
    public void case4_inclusive2() throws Throwable {
        Graph graph = Graph.fromUri("classpath:flow/script_case4.graph.json");

        FlowContext context = FlowContext.of();
        context.put("day", 7);
        flowEngine.eval(graph, context);
        assert context.getAs("result").equals(10);
    }

    @Test
    public void case5_parallel() throws Throwable {
        Graph graph = Graph.fromUri("classpath:flow/script_case5.graph.yml");

        FlowContext context = FlowContext.of();
        context.put("day", 7);
        flowEngine.eval(graph, context);
        assert context.getAs("result").equals(10);
    }

    @Test
    public void case8() throws Throwable {
        Graph graph = Graph.fromUri("classpath:flow/script_case8.graph.yml");

        FlowContext context = FlowContext.of();
        context.put("result", 1);
        flowEngine.eval(graph, context);
        assert context.getAs("result").equals(3);
    }

    @Test
    public void case9_parallel_async() throws Throwable {
        Graph graph = Graph.fromUri("classpath:flow/script_case9.graph.yml");

        FlowContext context = FlowContext.of();
        context.executor(Executors.newFixedThreadPool(4));

        flowEngine.eval(graph, context);
    }
}