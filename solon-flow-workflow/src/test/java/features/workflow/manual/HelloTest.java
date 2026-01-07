package features.workflow.manual;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.workflow.StateController;
import org.noear.solon.flow.workflow.StateRepository;
import org.noear.solon.flow.workflow.Task;
import org.noear.solon.flow.workflow.controller.NotBlockStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;
import org.noear.solon.flow.workflow.WorkflowExecutor;

/**
 *
 * @author noear 2025/12/5 created
 *
 */
public class HelloTest {
    @Test
    public void case1() {
        FlowEngine engine = FlowEngine.newInstance();
        StateController stateController = new NotBlockStateController();
        StateRepository stateRepository = new InMemoryStateRepository();

        Graph graph = Graph.create("c1", spec -> {
            spec.addStart("n1").linkAdd("n2");
            spec.addActivity("n2").task("System.out.println(\"hello world!\");").linkAdd("n3");
            spec.addExclusive("n3").linkAdd("n4");
            spec.addEnd("n4");
        });

        FlowContext context = FlowContext.of("i-1");
        Task task = WorkflowExecutor.of(engine, stateController, stateRepository)
                .claimTask(graph, context);

        Assertions.assertNull(task);
        Assertions.assertTrue(context.lastRecord().isEnd());
    }
}
