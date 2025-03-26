package demo.flow.async;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.Chain;
import org.noear.solon.flow.Container;
import org.noear.solon.flow.Evaluation;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.driver.AbstractChainDriver;
import org.noear.solon.flow.driver.SimpleChainDriver;
import org.noear.solon.flow.evaluation.LiquorEvaluation;

/**
 * @author noear 2025/3/20 created
 */
public class AsyncTest {
    @Test
    public void case1() throws Throwable {
        MapContainer container = new MapContainer();

        FlowEngine engine = FlowEngine.newInstance();
        engine.register(new SimpleChainDriver(container));

        container.putComponent("a", new TaskComponentImpl());

        engine.load(Chain.parseByUri("classpath:demo/async/async_case1.chain.yml"));

        engine.eval("c1");

        //因为是异步，所以要阻一下
        System.in.read();
    }
}
