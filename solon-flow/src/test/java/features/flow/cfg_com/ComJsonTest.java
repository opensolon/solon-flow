package features.flow.cfg_com;

import org.junit.jupiter.api.Test;
import org.noear.solon.SimpleSolonApp;
import org.noear.solon.flow.Chain;
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

        Chain chain = Chain.parseByUri("classpath:flow/com.chain.json");

        FlowContext context = FlowContext.of();
        context.put("a", 2);
        context.put("b", 3);
        context.put("c", 4);

        //完整执行

        flowEngine.eval(chain, context);
        System.out.println("------------");

        context = FlowContext.of();
        context.put("a", 12);
        context.put("b", 13);
        context.put("c", 14);

        //执行一层
        flowEngine.eval(chain.getNode("n2"), 1, context);
    }
}