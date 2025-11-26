package features.flow.stateful;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.stateful.FlowStatefulService;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.flow.stateful.controller.NotBlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

@Slf4j
public class NotBlockStateFlowTest2 {
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
    public void useEval() {
        //计算后，不能获取最新状态

        FlowEngine flowEngine = FlowEngine.newInstance();
        Graph graph = getGraph();

        FlowContext context = FlowContext.of("5", stateController, stateRepository)
                .put("tag", "");

        flowEngine.eval(graph, context);
        System.out.println("--------------------");

        context = FlowContext.of("6", stateController, stateRepository)
                .put("tag", "n1");
        flowEngine.eval(graph, context);
        System.out.println("--------------------");

        //再跑（仍在原位、原状态）
        flowEngine.eval(graph, context);
        System.out.println("--------------------");


        context.put("tag", "n2");
        flowEngine.eval(graph, context);
        System.out.println("--------------------");

        context.put("tag", "");
        flowEngine.eval(graph, context);
        System.out.println("--------------------");
    }

    @Test
    public void useFlowStatefulService() {
        //计算后，可获取最新状态

        FlowEngine flowEngine = FlowEngine.newInstance();
        FlowStatefulService statefulService = flowEngine.forStateful();
        Graph graph = getGraph();

        FlowContext context = FlowContext.of("3", stateController, stateRepository)
                .put("tag", "");

        StatefulTask task = statefulService.getTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n3", task.getNode().getId());
        Assertions.assertEquals(StateType.COMPLETED, task.getState());

        context = FlowContext.of("4", stateController, stateRepository)
                .put("tag", "n1");

        task = statefulService.getTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n1", task.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, task.getState());

        //再跑（仍在原位、原状态）
        task = statefulService.getTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n1", task.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, task.getState());


        context.put("tag", "n2");

        task = statefulService.getTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n2", task.getNode().getId());
        Assertions.assertEquals(StateType.WAITING, task.getState());

        context.put("tag", "");

        task = statefulService.getTask(graph, context);
        System.out.println("--------------------");
        Assertions.assertNotNull(task);
        Assertions.assertEquals("n3", task.getNode().getId());
        Assertions.assertEquals(StateType.COMPLETED, task.getState());
    }

    private Graph getGraph() {
        Graph graph = new GraphDecl("tmp-" + System.currentTimeMillis()).create(decl -> {
            String task = "if(tag.equals(node.getId())){exchanger.interrupt();}";

            decl.addNode(NodeDecl.startOf("s").linkAdd("n0"));
            decl.addNode(NodeDecl.activityOf("n0").task(task).linkAdd("n1"));
            decl.addNode(NodeDecl.activityOf("n1").task(task).linkAdd("n2"));
            decl.addNode(NodeDecl.activityOf("n2").task(task).linkAdd("n3"));
            decl.addNode(NodeDecl.activityOf("n3").task(task).linkAdd("e"));
            decl.addNode(NodeDecl.endOf("e"));
        });

        return graph;
    }
}