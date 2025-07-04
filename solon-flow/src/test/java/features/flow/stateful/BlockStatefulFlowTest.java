package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
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
        FlowEngine flowEngine = FlowEngine.newInstance(StatefulSimpleFlowDriver.builder()
                .stateController(new BlockStateController())
                .stateRepository(new InMemoryStateRepository())
                .build());

        flowEngine.load("classpath:flow/*.yml");

        StatefulService statefulService = flowEngine.stateful();

        FlowContext context;
        StatefulTask statefulNode;
        String instanceId1 = "i1";
        String instanceId2 = "i2";


        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step1");

        //根据节点干活。。。。

        //（干完后）提交操作
        statefulService.postOperation(context, statefulNode.getNode(), StateOperation.FORWARD);

        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step2");

        //根据节点干活。。。。

        //（干完后）提交操作
        statefulService.postOperation(context, statefulNode.getNode(), StateOperation.FORWARD);


        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step3");

        //根据节点干活。。。。

        //（干完后）提交操作
        statefulService.postOperation(context, statefulNode.getNode(), StateOperation.FORWARD);


        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step4_1");

        //根据节点干活

        //提交操作
        statefulService.postOperation(context, statefulNode.getNode(), StateOperation.FORWARD);


        ///  （换一个实例）

        context = new FlowContext(instanceId2);
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step1");

        statefulService.postOperation(context, statefulNode.getNode(), StateOperation.FORWARD);

        context = new FlowContext(instanceId2);
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step2");

        statefulService.postOperation(context, statefulNode.getNode(), StateOperation.FORWARD);


        context = new FlowContext(instanceId2);
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step3");


        // （再换回实例）

        //获取节点
        context = new FlowContext(instanceId1);
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step4_2");
    }

    @Test
    public void case2() throws Throwable {
        FlowEngine flowEngine = FlowEngine.newInstance(StatefulSimpleFlowDriver.builder()
                .stateController(new BlockStateController())
                .stateRepository(new InMemoryStateRepository())
                .build());

        flowEngine.load("classpath:flow/*.yml");

        StatefulService statefulService = flowEngine.stateful();

        StatefulTask statefulNode;
        String instanceId1 = "i3";

        //单步前进（上下文需要配置，实例id）
        statefulNode = statefulService.stepForward(chainId, new FlowContext(instanceId1)); //使用实例id
        assert "step1".equals(statefulNode.getNode().getId());

        statefulNode = statefulService.stepForward(chainId, new FlowContext(instanceId1)); //使用实例id
        assert "step2".equals(statefulNode.getNode().getId());

        statefulNode = statefulService.stepForward(chainId, new FlowContext(instanceId1)); //使用实例id
        assert "step3".equals(statefulNode.getNode().getId());

        //此时，已经是：step4_1 = WAITING
        statefulNode = statefulService.getTask(chainId, new FlowContext(instanceId1));
        assert "step4_1".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState();


        statefulNode = statefulService.stepBack(chainId, new FlowContext(instanceId1)); //使用实例id
        assert "step3".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState();

        statefulNode = statefulService.stepBack(chainId, new FlowContext(instanceId1)); //使用实例id
        assert "step2".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState();
    }

    private void assertNode(StatefulTask node, String id) {
        assert node.getState() == StateType.WAITING;
        assert id.equals(node.getNode().getId());
    }
}