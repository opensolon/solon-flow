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
            public void handleTask(FlowContext context, Task task) throws Throwable {
                context.result = task.getNode().getId();
                if (task.getNode().getId().equals("n3")) {
                    context.interrupt();
                }

                super.handleTask(context, task);
            }
        };

        flowEngine.register(driver);

        Chain chain = new Chain("c1", "c1");


        chain.addNode(new NodeDecl("n1", NodeType.start).linkAdd("n2"));
        chain.addNode(new NodeDecl("n2", NodeType.activity).task("@a").linkAdd("n3"));
        chain.addNode(new NodeDecl("n3", NodeType.activity).task("@b").linkAdd("n4"));
        chain.addNode(new NodeDecl("n4", NodeType.activity).task("@c").linkAdd("n5"));
        chain.addNode(new NodeDecl("n5", NodeType.end));

        FlowContext context = new FlowContext();
        context.put("a", 2);
        context.put("b", 3);
        context.put("c", 4);

        //完整执行

        flowEngine.eval(chain, context);

        assert "n3".equals(context.result);

        System.out.println("------------");

        context = new FlowContext();
        context.put("a", 12);
        context.put("b", 13);
        context.put("c", 14);

        //执行一层
        flowEngine.eval(chain.getNode("n2"), 1, context);


        assert "n2".equals(context.result);
    }
}