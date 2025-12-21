package features.flow.cfg_com;

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

        Graph graph = Graph.create("c1", decl -> {
            decl.addStart("n1").linkAdd("n2");
            decl.addActivity("n2").task("@a").linkAdd("n3");
            decl.addActivity("n3").task("@b").linkAdd("n4");
            decl.addActivity("n4").task("@c").linkAdd("n5");
            decl.addEnd("n5");
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
        flowEngine.eval(graph, "n2", 1, context);


        assert "n2".equals(context.getAs("result"));
    }
}