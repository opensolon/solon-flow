package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.stateful.FlowStatefulService;
import org.noear.solon.flow.stateful.StateRepository;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.flow.stateful.controller.BlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.noear.solon.test.SolonTest;

import java.util.Date;

@SolonTest
public class AutoForwardTest {

    StateRepository stateRepository = new InMemoryStateRepository();
    BlockStateController stateController = new BlockStateController() {
        @Override
        public boolean isAutoForward(FlowContext context, Node node) {
            return super.isAutoForward(context, node)
                    || node.getMetaOrDefault("auto", false)
                    || context.getOrDefault("all_auto", false);
        }
    };

    @Test
    public void case11() throws Exception {
        FlowStatefulService statefulService = FlowEngine.newInstance().forStateful();

        Graph graph = buildGraph();

        String graphId = "Test" + new Date().getTime();
        FlowContext context = FlowContext.of(graphId, stateController, stateRepository);

        context.put("all_auto", true);
        StatefulTask statefulNode = statefulService.stepForward(graph, context);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().equals("01");
        assert statefulNode.getState() == StateType.COMPLETED;

        statefulNode = statefulService.stepForward(graph, context);
        assert statefulNode == null;
    }

    private Graph buildGraph() {
        Graph graph = Graph.create("test_case11", "test_case11", decl -> {
            decl.addStart("start").title("开始").linkAdd("01");
            decl.addActivity("01").title("01").task("@oaMetaProcessCom").linkAdd("end");
            decl.addEnd("end").title("结束");
        });

        return graph;
    }
}