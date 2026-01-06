package features.flow.manual.cfg_com;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.noear.solon.SimpleSolonApp;
import org.noear.solon.flow.*;
import org.noear.solon.flow.driver.SimpleFlowDriver;

/**
 * 手动配装风格
 *
 * @author noear 2025/1/10 created
 */
@Slf4j
public class ComJavaTest {

    @Test
    public void case1() throws Throwable {
        FlowEngine flowEngine = FlowEngine.newInstance();

        SimpleSolonApp solonApp = new SimpleSolonApp(ComJavaTest.class);
        solonApp.start(null);

        SimpleFlowDriver driver = new SimpleFlowDriver() {
            @Override
            public void handleTask(FlowExchanger exchanger, TaskDesc task) throws Throwable {
                exchanger.context().put("result", task.getNode().getId());
                if (task.getNode().getId().equals("n3")) {
                    exchanger.interrupt();
                }

                super.handleTask(exchanger, task);
            }
        };

        flowEngine.register(driver);

        Graph graph = Graph.create("c1", spec -> {
            spec.addStart("n1").linkAdd("n2");
            spec.addActivity("n2").task("@a").linkAdd("n3");
            spec.addActivity("n3").task("@b").linkAdd("n4");
            spec.addActivity("n4").task("@c").linkAdd("n5");
            spec.addEnd("n5");
        });


        FlowContext context = FlowContext.of();
        context.put("a", 2);
        context.put("b", 3);
        context.put("c", 4);

        //完整执行

        flowEngine.eval(graph, context);

        assert "n3".equals(context.getAs("result"));

        System.out.println("------------");

        context = FlowContext.of();
        context.put("a", 12);
        context.put("b", 13);
        context.put("c", 14);

        //执行一层
        context.trace().recordNodeId(graph, "n2");
        flowEngine.eval(graph, 1, context);


        assert "n2".equals(context.getAs("result"));
    }
}