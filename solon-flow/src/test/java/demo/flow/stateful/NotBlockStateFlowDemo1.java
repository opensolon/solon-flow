package demo.flow.stateful;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.controller.NotBlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

/**
 *
 * @author noear 2025/12/20 created
 *
 */
@Slf4j
public class NotBlockStateFlowDemo1 {
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