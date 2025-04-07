package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.stateful.*;
import org.noear.solon.flow.stateful.controller.BlockStateController;
import org.noear.solon.flow.stateful.driver.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

/**
 * @author noear 2025/3/28 created
 */
public class BlockStatefulFlowTest {
    final String chainId = "sf2";

    @Test
    public void case1() throws Throwable {
        StatefulFlowEngine flowEngine = new StatefulFlowEngineDefault(StatefulSimpleFlowDriver.builder()
                .stateController(new BlockStateController())
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
        flowEngine.postActivityState(context, statefulNode.getNode(), StateType.COMPLETED);

        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step2");

        //根据节点干活。。。。

        //（干完后）提交节点状态
        flowEngine.postActivityState(context, statefulNode.getNode(), StateType.COMPLETED);


        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step3");

        //根据节点干活。。。。

        //（干完后）提交节点状态
        flowEngine.postActivityState(context, statefulNode.getNode(), StateType.COMPLETED);


        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step4_1");

        //根据节点干活

        //提交节点状态
        flowEngine.postActivityState(context, statefulNode.getNode(), StateType.COMPLETED);


        ///  （换一个实例）

        context = new FlowContext(instanceId2);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step1");

        flowEngine.postActivityState(context, statefulNode.getNode(), StateType.COMPLETED);

        context = new FlowContext(instanceId2);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step2");

        flowEngine.postActivityState(context, statefulNode.getNode(), StateType.COMPLETED);


        context = new FlowContext(instanceId2);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step3");


        // （再换回实例）

        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = flowEngine.getActivityNode(chainId, context);
        assertNode(statefulNode, "step4_2");
    }

    @Test
    public void case2() throws Throwable {
        StatefulFlowEngineDefault flowEngine = new StatefulFlowEngineDefault(StatefulSimpleFlowDriver.builder()
                .stateController(new BlockStateController())
                .stateRepository(new InMemoryStateRepository())
                .build());

        flowEngine.load("classpath:flow/*.yml");

        StatefulNode statefulNode;
        String instanceId1 = "i3";

        //单步前进（上下文需要配置，实例id）
        statefulNode = flowEngine.stepForward(chainId, new FlowContext(instanceId1)); //使用实例id
        assert "step1".equals(statefulNode.getNode().getId());

        statefulNode = flowEngine.stepForward(chainId, new FlowContext(instanceId1)); //使用实例id
        assert "step2".equals(statefulNode.getNode().getId());

        statefulNode = flowEngine.stepForward(chainId, new FlowContext(instanceId1)); //使用实例id
        assert "step3".equals(statefulNode.getNode().getId());

        //此时，已经是：step4_1 = WAITING
        statefulNode = flowEngine.getActivityNode(chainId, new FlowContext(instanceId1));
        assert "step4_1".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState();


        statefulNode = flowEngine.stepBack(chainId, new FlowContext(instanceId1)); //使用实例id
        assert "step3".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState();

        statefulNode = flowEngine.stepBack(chainId, new FlowContext(instanceId1)); //使用实例id
        assert "step2".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState();
    }

    private void assertNode(StatefulNode node, String id) {
        assert node.getState() == StateType.WAITING;
        assert id.equals(node.getNode().getId());
    }
}