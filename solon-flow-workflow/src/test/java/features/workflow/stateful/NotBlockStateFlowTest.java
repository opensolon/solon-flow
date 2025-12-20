package features.workflow.stateful;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.FlowException;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.intercept.FlowInterceptor;
import org.noear.solon.flow.intercept.FlowInvocation;
import org.noear.solon.flow.workflow.TaskState;
import org.noear.solon.flow.workflow.Task;
import org.noear.solon.flow.workflow.controller.NotBlockStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;
import org.noear.solon.flow.workflow.WorkflowService;

@Slf4j
public class NotBlockStateFlowTest {
    final int amount = 900000;
    final String graphId = "test1";

    NotBlockStateController stateController = new NotBlockStateController();
    InMemoryStateRepository stateRepository = new InMemoryStateRepository() {
        @Override
        public void statePut(FlowContext context, Node node, TaskState state) {
            super.statePut(context, node, state);
            //todo: 打印放这儿，顺序更真实
            if (state == TaskState.COMPLETED) {
                log.info("{} 完成", node.getId());
            }
        }
    };

    @Test
    public void case1() {
        FlowEngine flowEngine = FlowEngine.newInstance();

        flowEngine.load("classpath:flow/stateful/*.yml");

        WorkflowService workflow = WorkflowService.of(flowEngine, stateController, stateRepository);


        /// ////////////

        FlowContext context = FlowContext.of("1")
                .put("amount", amount);
        Task task;


        task = workflow.getTask(graphId, context);

        Assertions.assertEquals("n5", task.getNode().getId());
        Assertions.assertEquals(TaskState.COMPLETED, task.getState());
    }

    @Test
    public void caseInterceptorBlock() {
        FlowInterceptor interceptor = new FlowInterceptor() {
            @Override
            public void doIntercept(FlowInvocation invocation) throws FlowException {
                invocation.invoke();
            }

            @Override
            public void onNodeStart(FlowContext context, Node node) {
                if (node.getId().equals("n1")) {
                    context.exchanger().interrupt();
                }
            }
        };
        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.addInterceptor(interceptor);

        flowEngine.load("classpath:flow/stateful/*.yml");

        WorkflowService workflow = WorkflowService.of(flowEngine, stateController, stateRepository);


        /// ////////////

        FlowContext context = FlowContext.of("2")
                .put("amount", amount);
        Task task;


        task = workflow.getTask(graphId, context);

        Assertions.assertEquals("n0", task.getNode().getId());
        Assertions.assertEquals(TaskState.COMPLETED, task.getState());


        task = workflow.getTask(graphId, context);

        Assertions.assertNull(task); //提前中断，没有节点可取了

        System.out.println("---------------------");

        flowEngine.removeInterceptor(interceptor);

        task = workflow.getTask(graphId, context);

        Assertions.assertEquals("n5", task.getNode().getId());
        Assertions.assertEquals(TaskState.COMPLETED, task.getState());


        task = workflow.getTask(graphId, context);

        Assertions.assertNull(task); //全部完成，没有节点可取了
    }
}