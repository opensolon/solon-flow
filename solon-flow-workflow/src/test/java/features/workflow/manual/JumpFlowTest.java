package features.workflow.manual;

import org.junit.jupiter.api.Test;
import org.noear.solon.Utils;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.workflow.TaskAction;
import org.noear.solon.flow.workflow.StateRepository;
import org.noear.solon.flow.workflow.TaskState;
import org.noear.solon.flow.workflow.Task;
import org.noear.solon.flow.workflow.controller.ActorStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;
import org.noear.solon.flow.workflow.WorkflowExecutor;
import org.noear.solon.test.SolonTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author noear 2025/8/4 created
 */
@SolonTest
public class JumpFlowTest {
    static final Logger log = LoggerFactory.getLogger(JumpFlowTest.class);

    final String graphId = "test2";
    final String instanceId = Utils.uuid();
    final String actor = "role";

    StateRepository stateRepository = new InMemoryStateRepository();
    ActorStateController stateController = new ActorStateController(actor) {
        @Override
        public boolean isOperatable(FlowContext context, Node node) {
            if ("admin".equals(context.getAs(actor))) {
                return true;
            }

            return super.isOperatable(context, node);
        }
    };

    private WorkflowExecutor buildWorkflow() {
        MapContainer container = new MapContainer();
        FlowEngine fe = FlowEngine.newInstance(SimpleFlowDriver.builder()
                .container(container)
                .build());


        fe.load("classpath:flow/workflow/*.yml");

        return WorkflowExecutor.of(fe, stateController, stateRepository);
    }

    @Test
    public void case1() {
        WorkflowExecutor workflow = buildWorkflow();
        FlowContext context = FlowContext.of(instanceId).put(actor, "admin");

        workflow.submitTask(graphId, "n3", TaskAction.FORWARD_JUMP, context);

        Task task = workflow.claimTask(graphId, context);

        log.debug(task.toString());
        assert task.getState() == TaskState.WAITING;
        assert task.getNode().getId().equals("n4");


        workflow.submitTask(graphId, "n1", TaskAction.BACK_JUMP, context);

        task = workflow.claimTask(graphId, context);

        log.debug(task.toString());
        assert task.getState() == TaskState.WAITING;
        assert task.getNode().getId().equals("n0");
    }
}