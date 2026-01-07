package demo.workflow;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.workflow.TaskState;
import org.noear.solon.flow.workflow.Task;
import org.noear.solon.flow.workflow.controller.NotBlockStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;
import org.noear.solon.flow.workflow.WorkflowExecutor;

/**
 *
 * @author noear 2025/12/20 created
 *
 */
@Slf4j
public class NotBlockStateFlowDemo2 {
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
        //计算后，可获取最新状态

        WorkflowExecutor workflow = WorkflowExecutor.of(FlowEngine.newInstance(), stateController, stateRepository);
        Graph graph = getGraph();

        FlowContext context = FlowContext.of("3")
                .put("tag", "");

        Task task = workflow.matchTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n3", task.getNode().getId());
        Assertions.assertEquals(TaskState.COMPLETED, task.getState());

        context = FlowContext.of("4")
                .put("tag", "n1");

        task = workflow.matchTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n1", task.getNode().getId());
        Assertions.assertEquals(TaskState.WAITING, task.getState());

        //再跑（仍在原位、原状态）
        task = workflow.matchTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n1", task.getNode().getId());
        Assertions.assertEquals(TaskState.WAITING, task.getState());


        context.put("tag", "n2");

        task = workflow.matchTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n2", task.getNode().getId());
        Assertions.assertEquals(TaskState.WAITING, task.getState());

        context.put("tag", "");

        task = workflow.matchTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n3", task.getNode().getId());
        Assertions.assertEquals(TaskState.COMPLETED, task.getState());
    }

    private Graph getGraph() {
        String task = "if(tag.equals(node.getId())){exchanger.interrupt();}";

        Graph graph = Graph.create("tmp-" + System.currentTimeMillis(),spec->{
            spec.addStart("s").linkAdd("n0");
            spec.addActivity("n0").task(task).linkAdd("n1");
            spec.addActivity("n1").task(task).linkAdd("n2");
            spec.addActivity("n2").task(task).linkAdd("n3");
            spec.addActivity("n3").task(task).linkAdd("e");
            spec.addEnd("e");
        });

        return graph;
    }
}
