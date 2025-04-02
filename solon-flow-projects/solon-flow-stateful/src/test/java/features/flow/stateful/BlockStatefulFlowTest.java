package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.stateful.*;
import org.noear.solon.flow.stateful.operator.BlockStateOperator;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

/**
 * @author noear 2025/3/28 created
 */
public class BlockStatefulFlowTest {
    final String chainId = "f2";

    @Test
    public void case1() throws Throwable {
        StatefulFlowEngine flowEngine = new StatefulFlowEngine(StatefulSimpleFlowDriver.builder()
                .stateOperator(new BlockStateOperator())
                .stateRepository(new InMemoryStateRepository())
                .build());

        flowEngine.load("classpath:flow/*.yml");

        FlowContext context;
        StatefulNode statefulNode;
        String instanceId1 = "i1";
        String instanceId2 = "i2";


        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step1");

        //根据节点干活。。。。

        //（干完后）提交节点状态
        flowEngine.postActivityState(context, statefulNode.getNode(), NodeState.COMPLETED);

        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step2");

        //根据节点干活。。。。

        //（干完后）提交节点状态
        flowEngine.postActivityState(context, statefulNode.getNode(), NodeState.COMPLETED);


        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step3");

        //根据节点干活。。。。

        //（干完后）提交节点状态
        flowEngine.postActivityState(context, statefulNode.getNode(), NodeState.COMPLETED);


        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step4_1");

        //根据节点干活

        //提交节点状态
        flowEngine.postActivityState(context, statefulNode.getNode(), NodeState.COMPLETED);


        ///  （换一个实例）

        context = new FlowContext(instanceId2);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step1");

        flowEngine.postActivityState(context, statefulNode.getNode(), NodeState.COMPLETED);

        context = new FlowContext(instanceId2);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step2");

        flowEngine.postActivityState(context, statefulNode.getNode(), NodeState.COMPLETED);


        context = new FlowContext(instanceId2);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step3");


        // （再换回实例）

        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step4_2");
    }

    private void assertNode(StatefulNode node, String id)  {
        assert node.getState() == NodeState.WAITING;
        assert id.equals(node.getNode().getId());
    }
}