package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.stateful.FlowStatefulService;
import org.noear.solon.flow.stateful.StateType;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.flow.stateful.controller.BlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.noear.solon.test.SolonTest;

import java.util.Date;

@SolonTest
public class AutoForwardTest {

    BlockStateController stateController = new BlockStateController() {
        @Override
        public boolean isAutoForward(FlowContext context, Node node) {
            return super.isAutoForward(context, node)
                    || node.getMetaOrDefault("auto", false)
                    || context.getOrDefault("all_auto", false);
        }
    };
    InMemoryStateRepository stateRepository = new InMemoryStateRepository();

    @Test
    public void case11() throws Exception {
        FlowStatefulService statefulService = FlowEngine.newInstance().statefulService();

        Chain chain = buildChain();

        String chainId = "Test" + new Date().getTime();
        FlowContext context = FlowContext.of(chainId, stateController, stateRepository);
        context.put("all_auto", true);
        StatefulTask statefulNode = statefulService.stepForward(chain, context);
        assert statefulNode != null;
        assert statefulNode.getNode().getId().equals("01");
        assert statefulNode.getState() == StateType.COMPLETED;

        statefulNode = statefulService.stepForward(chain, context);
        assert statefulNode == null;
    }

    private Chain buildChain() {
        Chain chain = new Chain("test_case11", "test_case11");
        NodeDecl nodeDecl = null;

        nodeDecl = new NodeDecl("start", NodeType.START).title("开始").linkAdd("01");
        chain.addNode(nodeDecl);


        nodeDecl = new NodeDecl("01", NodeType.ACTIVITY).title("01").linkAdd("end")
        //.metaPut("auto", true)
        ;
        nodeDecl.task("@oaMetaProcessCom");
        chain.addNode(nodeDecl);

        nodeDecl = new NodeDecl("end", NodeType.END).title("结束");
        chain.addNode(nodeDecl);

        return chain;
    }
}