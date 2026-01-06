package features.flow.generated.coverage;

import org.junit.jupiter.api.BeforeEach;
import org.noear.solon.flow.Evaluation;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.evaluation.LiquorEvaluation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 测试基础类
 */
public abstract class FlowTestBase {
    protected FlowEngine flowEngine;
    protected SimpleFlowDriver flowDriver;
    protected MapContainer container;
    protected ExecutorService executorService;
    protected Evaluation evaluation;

    @BeforeEach
    void setUp() {
        evaluation = new LiquorEvaluation();
        container = new MapContainer();
        executorService = Executors.newFixedThreadPool(2);
        flowDriver = SimpleFlowDriver.builder()
                .evaluation(evaluation)
                .container(container)
                .executor(executorService)
                .build();
        flowEngine = FlowEngine.newInstance(flowDriver);
    }
}