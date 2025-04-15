package features.flow.event;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.TaskComponent;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.driver.SimpleFlowDriver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author noear 2025/4/15 created
 */
public class EventTest {
    @Test
    public void case1() throws Exception {
        MapContainer container = new MapContainer();
        container.putComponent("DemoCom", new DemoCom());

        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.register(new SimpleFlowDriver(container));
        flowEngine.load("classpath:flow/*.yml");


        CountDownLatch latch = new CountDownLatch(2);

        FlowContext context = new FlowContext();
        context.eventBus().listen("demo.topic", event -> {
            System.out.println(event.getContent());
            latch.countDown();
        });

        flowEngine.eval("event1", context);

        assert latch.getCount() == 0;
    }

    public static class DemoCom implements TaskComponent {

        @Override
        public void run(FlowContext context, Node node) throws Throwable {
            context.eventBus().send("demo.topic", "hello-com");
        }
    }
}
