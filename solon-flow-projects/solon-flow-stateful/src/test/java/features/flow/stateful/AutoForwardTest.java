package features.flow.stateful;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.Chain;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.NodeDecl;
import org.noear.solon.flow.NodeType;
import org.noear.solon.flow.stateful.StatefulFlowEngine;
import org.noear.solon.flow.stateful.StatefulNode;
import org.noear.solon.flow.stateful.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.operator.BlockStateOperator;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;
import org.noear.solon.test.SolonTest;

@SolonTest
public class AutoForwardTest {

    @Test
    public void case11() throws Exception {
        StatefulFlowEngine flowEngine = new StatefulFlowEngine(StatefulSimpleFlowDriver.builder()
                .stateOperator(new BlockStateOperator() {
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

        FlowContext context = new FlowContext("Test"+new Date().getTime());
        context.put("all_auto", true);

        StatefulNode statefulNode = flowEngine.stepForward(chain, context);

        context = new FlowContext("Test"+new Date().getTime());
        statefulNode = flowEngine.stepForward(chain, context);
        assert statefulNode==null;
    }

    private Chain buildChain() {
        Chain chain = new Chain("test_case11","test_case11");
        NodeDecl nodeDecl = null;

        nodeDecl = new NodeDecl("start",NodeType.START).title("开始").linkAdd("01");
        chain.addNode(nodeDecl);

        // .metaPut("auto", true)
        nodeDecl = new NodeDecl("01",NodeType.ACTIVITY).title("01").linkAdd("end");
        nodeDecl.task("@oaMetaProcessCom");
        chain.addNode(nodeDecl);

        nodeDecl = new NodeDecl("end",NodeType.END).title("结束");
        chain.addNode(nodeDecl);

        return chain;
    }
}
