package features.flow.generated.coverage;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;

import org.noear.solon.flow.*;
import org.noear.solon.flow.driver.AbstractFlowDriver;
import org.noear.solon.flow.driver.SimpleFlowDriver;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimpleFlowDriver 单元测试
 */
class SimpleFlowDriverTest extends FlowTestBase {

    private SimpleFlowDriver driver;

    @BeforeEach
    @Override
    void setUp() {
        super.setUp();
        driver = (SimpleFlowDriver) flowDriver;
    }

    @Test
    void testSingletonInstance() {
        FlowDriver instance1 = SimpleFlowDriver.getInstance();
        FlowDriver instance2 = SimpleFlowDriver.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testConstructorVariants() {
        // 测试各种构造函数
        SimpleFlowDriver driver1 = new SimpleFlowDriver();
        assertNotNull(driver1);

        SimpleFlowDriver driver2 = new SimpleFlowDriver(evaluation);
        assertNotNull(driver2);

        SimpleFlowDriver driver3 = new SimpleFlowDriver(container);
        assertNotNull(driver3);

        SimpleFlowDriver driver4 = new SimpleFlowDriver(evaluation, container);
        assertNotNull(driver4);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        SimpleFlowDriver driver5 = new SimpleFlowDriver(evaluation, container, executor);
        assertNotNull(driver5);
        assertSame(executor, driver5.getExecutor());
    }

    @Test
    void testBuilderPattern() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        SimpleFlowDriver builtDriver = SimpleFlowDriver.builder()
                .evaluation(evaluation)
                .container(container)
                .executor(executor)
                .build();

        assertNotNull(builtDriver);
        assertSame(executor, builtDriver.getExecutor());
    }

    @Test
    void testHandleTask() throws Throwable {
        Graph graph = Graph.create("test", spec -> {
            spec.addStart("s").linkAdd("a");
            spec.addActivity("a").task("context.put(\"handled\", true)").linkAdd("e");
            spec.addEnd("e");
        });

        FlowContext context = FlowContext.of();
        FlowEngine engine = FlowEngine.newInstance(driver);
        FlowExchanger exchanger = new FlowExchanger(engine, driver, context, -1, null);

        Node node = graph.getNode("a");
        TaskDesc task = node.getTask();

        driver.handleTask(exchanger, task);

        assertTrue(context.<Boolean>getAs("handled"));
    }
}