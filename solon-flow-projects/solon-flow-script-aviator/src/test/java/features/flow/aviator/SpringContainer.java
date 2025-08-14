package features.flow.aviator;

import org.noear.solon.flow.Container;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.driver.SimpleFlowDriver;

/**
 * @author noear 2025/3/26 created
 */
public class SpringContainer implements Container {
    @Override
    public Object getComponent(String componentName) {
        return null;
    }

    public static void demo() throws Throwable {
        FlowEngine engine = FlowEngine.newInstance();
        engine.register(new SimpleFlowDriver(new SpringContainer()));

        engine.load("classpath:flow/*");

        FlowContext context = FlowContext.of();
        context.put("a", 1);
        context.put("b", 2);

        engine.eval("f1", context);
    }
}
