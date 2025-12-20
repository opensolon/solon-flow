package demo.flow.stateful;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.flow.stateful.controller.NotBlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

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
        public void statePut(FlowContext context, Node node, StateType state) {
            super.statePut(context, node, state);
            //todo: 打印放这儿，顺序更真实
            if (state == StateType.COMPLETED) {
                log.info("{} 完成", node.getId());
            }
        }
    };

    @Test
    public void useFlowStateful() {
        //计算后，可获取最新状态

        FlowEngine flowEngine = FlowEngine.newInstance();
        Graph graph = getGraph();

        FlowContext context = FlowContext.of("3", stateController, stateRepository)
                .put("tag", "");

        StatefulTask task = flowEngine.forStateful().getTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n3", task.getNode().getId());
        Assertions.assertEquals(StateType.COMPLETED, task.getState());

        context = FlowContext.of("4", stateController, stateRepository)
                .put("tag", "n1");

        task = flowEngine.forStateful().getTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n1", task.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, task.getState());

        //再跑（仍在原位、原状态）
        task = flowEngine.forStateful().getTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n1", task.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, task.getState());


        context.put("tag", "n2");

        task = flowEngine.forStateful().getTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n2", task.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, task.getState());

        context.put("tag", "");

        task = flowEngine.forStateful().getTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n3", task.getNode().getId());
        Assertions.assertEquals(StateType.COMPLETED, task.getState());
    }

    private Graph getGraph() {
        String task = "if(tag.equals(node.getId())){exchanger.interrupt();}";

        Graph graph = Graph.create("tmp-" + System.currentTimeMillis(),decl->{
            decl.addStart("s").linkAdd("n0");
            decl.addActivity("n0").task(task).linkAdd("n1");
            decl.addActivity("n1").task(task).linkAdd("n2");
            decl.addActivity("n2").task(task).linkAdd("n3");
            decl.addActivity("n3").task(task).linkAdd("e");
            decl.addEnd("e");
        });

        return graph;
    }
}
