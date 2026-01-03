package features.flow.cfg_com;

import org.junit.jupiter.api.Test;
import org.noear.solon.SimpleSolonApp;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;

/**
 * 手动配装风格
 *
 * @author noear 2025/1/10 created
 */
public class ComJsonTest {
    private FlowEngine flowEngine = FlowEngine.newInstance();

    @Test
    public void case1() throws Throwable {
        SimpleSolonApp solonApp = new SimpleSolonApp(ComJsonTest.class);
        solonApp.start(null);

        Graph graph = Graph.fromUri("classpath:flow/com.graph.json");

        FlowContext context = FlowContext.of();
        context.put("a", 2);
        context.put("b", 3);
        context.put("c", 4);

        //完整执行

        flowEngine.eval(graph, context);
        System.out.println("------------");

        context = FlowContext.of();
        context.put("a", 12);
        context.put("b", 13);
        context.put("c", 14);

        //执行一层
        context.trace().recordNodeId(graph, "n2");
        flowEngine.eval(graph, 1, context);
    }
}