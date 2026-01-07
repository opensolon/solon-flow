package features.workflow.manual;

import org.junit.jupiter.api.Test;
import org.noear.solon.Utils;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.container.MapContainer;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.noear.solon.flow.workflow.TaskAction;
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
public class JumpFlowTest2 {
    static final Logger log = LoggerFactory.getLogger(JumpFlowTest2.class);

    final String graphId = "test3";
    final String instanceId = Utils.uuid();
    final String actor = "role";

    ActorStateController stateController = new ActorStateController(actor) {
        @Override
        public boolean isOperatable(FlowContext context, Node node) {
            if ("admin".equals(context.getAs(actor))) {
                return true;
            }

            return super.isOperatable(context, node);
        }
    };
    InMemoryStateRepository stateRepository = new InMemoryStateRepository();

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

        Task task = workflow.matchTask(graphId, context);

        log.debug(task.toString());
        assert task.getState() == TaskState.WAITING;
        assert task.getNode().getId().equals("n4");


        workflow.submitTask(graphId, "n1", TaskAction.BACK_JUMP, context);

        task = workflow.matchTask(graphId, context);

        log.debug(task.toString());
        assert task.getState() == TaskState.WAITING;
        assert task.getNode().getId().equals("n0");
    }

    @Test
    public void case2() {
        WorkflowExecutor workflow = buildWorkflow();
        FlowContext context = FlowContext.of(instanceId).put(actor, "admin");

        Task task = workflow.matchTask(graphId, context);
        log.debug(task.toString());

        workflow.submitTask(task.getNode(), TaskAction.FORWARD, context);
        Task task2 = workflow.matchTask(graphId, context);
        log.debug(task2.toString());

        workflow.submitTask(task.getNode(), TaskAction.FORWARD, context);
        Task task3 = workflow.matchTask(graphId, context);
        log.debug(task3.toString());

        //重复提交相同节点后，获取的任务仍是相同的（说明可以重复提交）
        assert task2.getNode().getId().equals(task3.getNode().getId());
    }

    @Test
    public void case3() throws Throwable {
        WorkflowExecutor workflow = buildWorkflow();
        FlowContext context = FlowContext.of(instanceId).put(actor, "admin");

        Task task = workflow.matchTask(graphId, context);
        log.debug(task.toString());

        task.run(context);
    }
}