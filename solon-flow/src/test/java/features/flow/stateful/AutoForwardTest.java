package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.stateful.StatefulFlowEngine;
import org.noear.solon.flow.stateful.StatefulFlowEngineDefault;
import org.noear.solon.flow.stateful.StatefulNode;
import org.noear.solon.flow.stateful.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.controller.BlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.noear.solon.test.SolonTest;

import java.util.Date;

@SolonTest
public class AutoForwardTest {

    @Test
    public void case11() throws Exception {
        StatefulFlowEngine flowEngine = new StatefulFlowEngineDefault(StatefulSimpleFlowDriver.builder()
                .stateController(new BlockStateController() {
                    @Override
                    public boolean isAutoForward(FlowContext context, Node node) {
                        return super.isAutoForward(context, node)
                                || node.getMetaOrDefault("auto", false)
                                || context.getOrDefault("all_auto", false);
                    }
                }) // 换了一个
                .stateRepository(new InMemoryStateRepository())
                .build());

        Chain chain = buildChain();

        String chainId = "Test"+new Date().getTime();
        FlowContext context = new FlowContext(chainId);
        context.put("all_auto", true);
        StatefulNode statefulNode = flowEngine.stepForward(chain, context);
        assert statefulNode==null;

        context = new FlowContext(chainId);
        context.put("all_auto", true);
        statefulNode = flowEngine.stepForward(chain, context);
        assert statefulNode==null;
    }

    private Chain buildChain() {
        Chain chain = new Chain("test_case11","test_case11");
        NodeDecl nodeDecl = null;

        nodeDecl = new NodeDecl("start",NodeType.START).title("开始").linkAdd("01");
        chain.addNode(nodeDecl);

        
        nodeDecl = new NodeDecl("01",NodeType.ACTIVITY).title("01").linkAdd("end")
        //.metaPut("auto", true)
        ;
        nodeDecl.task("@oaMetaProcessCom");
        chain.addNode(nodeDecl);

        nodeDecl = new NodeDecl("end",NodeType.END).title("结束");
        chain.addNode(nodeDecl);

        return chain;
    }
}
