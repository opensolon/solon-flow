package features.flow.event;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.driver.SimpleFlowDriver;

import java.util.concurrent.CountDownLatch;

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


        CountDownLatch latch = new CountDownLatch(3);

        FlowContext context = FlowContext.of();
        context.eventBus().listen("demo.topic", event -> {
            System.out.println(event.getPayload());
            latch.countDown();
        });

        //context.<String,String>eventBus(); //泛型模式

        flowEngine.eval("event1", context);

        assert latch.getCount() == 0;
    }

    public static class DemoCom implements TaskComponent {

        @Override
        public void run(FlowContext context, Node node) throws Throwable {
            //通用类型模式
            context.eventBus().send("demo.topic", "hello-com");

            //泛型模式
            context.<String, String>eventBus().send("demo.topic", "hello-com2");
        }
    }
}
