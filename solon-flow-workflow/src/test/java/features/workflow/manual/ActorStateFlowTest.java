package features.workflow.manual;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.Utils;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.workflow.TaskAction;
import org.noear.solon.flow.workflow.TaskState;
import org.noear.solon.flow.workflow.Task;
import org.noear.solon.flow.workflow.controller.ActorStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;
import org.noear.solon.flow.workflow.WorkflowExecutor;

import java.util.Collection;

@Slf4j
public class ActorStateFlowTest {
    final String instanceId = Utils.uuid();
    final int amount = 900000;
    final String graphId = "test1";

    ActorStateController stateController = new ActorStateController("role");
    InMemoryStateRepository stateRepository = new InMemoryStateRepository() {
        @Override
        public void statePut(FlowContext context, Node node, TaskState state) {
            super.statePut(context, node, state);
            //todo: 打印放这儿，顺序更真实
            if (state == TaskState.COMPLETED) {
                log.info("{} {} 完成", node.getId(), node.getTitle());
            }
        }
    };

    @Test
    public void case1() {
        FlowEngine flowEngine = FlowEngine.newInstance();

        flowEngine.load("classpath:flow/workflow/*.yml");

        WorkflowExecutor workflow = WorkflowExecutor.of(flowEngine, stateController, stateRepository);


        /// ////////////

        FlowContext context;
        Task task;


        context = getFlowContext("employee");
        task = workflow.matchTask(graphId, context);
        Assertions.assertEquals("n0", task.getNode().getId());
        Assertions.assertEquals(TaskState.WAITING, task.getState());
        workflow.submitTask(graphId, task.getNodeId(), TaskAction.FORWARD, context);


        context = getFlowContext("tl");
        task = workflow.matchTask(graphId, context);
        Assertions.assertEquals("n1", task.getNode().getId());
        Assertions.assertEquals(TaskState.WAITING, task.getState());
        workflow.submitTask(graphId, task.getNodeId(), TaskAction.FORWARD, context);


        context = getFlowContext("dm");
        Collection<Task> tasks = workflow.findNextTasks(graphId, context);
        for (Task task1 : tasks) {
            context = getFlowContext("dm");
            context.put("amount", amount);
            workflow.submitTask(task1, TaskAction.FORWARD, context);
        }

        context = getFlowContext("oa");
        task = workflow.matchTask(graphId, context);
        Assertions.assertNull(task, "必须为End节点");

    }

    private FlowContext getFlowContext(String role) {
        return FlowContext.of(instanceId).put("role", role).put("amount", amount);
    }

    private Collection<Task> getEmailNode(WorkflowExecutor flowEngine) {
        FlowContext flowContext = getFlowContext("oa");
        return flowEngine.findNextTasks(graphId, flowContext);
    }
}