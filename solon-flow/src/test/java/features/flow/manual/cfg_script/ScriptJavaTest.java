package features.flow.manual.cfg_script;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;

/**
 * 手动配装风格
 *
 * @author noear 2025/1/10 created
 */
public class ScriptJavaTest {
    private FlowEngine flowEngine = FlowEngine.newInstance();

    @Test
    public void case1() throws Throwable {
        Graph graph = Graph.create("c1",spec -> {
            spec.addStart("n1").linkAdd("n2");
            spec.addActivity("n2").task("context.put(\"result\", 111 + a);").linkAdd("n3");
            spec.addEnd("n2");
        });

        FlowContext context = FlowContext.of();
        context.put("a", 2);
        context.put("b", 3);
        context.put("c", 4);

        flowEngine.eval(graph, context);
    }
}