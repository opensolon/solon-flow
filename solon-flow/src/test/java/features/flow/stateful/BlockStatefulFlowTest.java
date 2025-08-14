package features.flow.stateful;

import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.stateful.*;
import org.noear.solon.flow.stateful.controller.BlockStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

/**
 * @author noear 2025/3/28 created
 */
public class BlockStatefulFlowTest {
    final String chainId = "sf2";


    @Test
    public void case1() throws Throwable {
        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.load("classpath:flow/*.yml");

        BlockStateController stateController = new BlockStateController();
        InMemoryStateRepository stateRepository = new InMemoryStateRepository();
        FlowStatefulService statefulService = flowEngine.statefulService();


        StatefulTask statefulNode;
        String instanceId1 = "i1";
        String instanceId2 = "i2";

        FlowContext context = FlowContext.of(instanceId1, stateController, stateRepository);

        //获取节点
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step1");

        //根据节点干活。。。。

        //（干完后）提交操作
        statefulService.postOperation(statefulNode.getNode(), Operation.FORWARD, context);

        //获取节点
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step2");

        //根据节点干活。。。。

        //（干完后）提交操作
        statefulService.postOperation(statefulNode.getNode(), Operation.FORWARD, context);


        //获取节点
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step3");

        //根据节点干活。。。。

        //（干完后）提交操作
        statefulService.postOperation(statefulNode.getNode(), Operation.FORWARD, context);


        //获取节点
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step4_1");

        //根据节点干活

        //提交操作
        statefulService.postOperation(statefulNode.getNode(), Operation.FORWARD, context);


        ///  （换一个实例）

        context = FlowContext.of(instanceId2, stateController, stateRepository);
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step1");

        statefulService.postOperation(statefulNode.getNode(), Operation.FORWARD, context);

        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step2");

        statefulService.postOperation(statefulNode.getNode(), Operation.FORWARD, context);


        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step3");


        // （再换回实例）

        //获取节点
        context = FlowContext.of(instanceId1, stateController, stateRepository);
        statefulNode = statefulService.getTask(chainId, context);
        assertNode(statefulNode, "step4_2");
    }

    @Test
    public void case2() throws Throwable {
        FlowEngine flowEngine = FlowEngine.newInstance();
        flowEngine.load("classpath:flow/*.yml");

        BlockStateController stateController = new BlockStateController();
        InMemoryStateRepository stateRepository = new InMemoryStateRepository();
        FlowStatefulService statefulService = flowEngine.statefulService();

        StatefulTask statefulNode;
        String instanceId1 = "i3";

        FlowContext context = FlowContext.of(instanceId1, stateController, stateRepository);

        //单步前进（上下文需要配置，实例id）
        statefulNode = statefulService.stepForward(chainId, context); //使用实例id
        assert "step1".equals(statefulNode.getNode().getId());

        statefulNode = statefulService.stepForward(chainId, context); //使用实例id
        assert "step2".equals(statefulNode.getNode().getId());

        statefulNode = statefulService.stepForward(chainId, context); //使用实例id
        assert "step3".equals(statefulNode.getNode().getId());

        //此时，已经是：step4_1 = WAITING
        statefulNode = statefulService.getTask(chainId, context);
        assert "step4_1".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState();


        statefulNode = statefulService.stepBack(chainId, context); //使用实例id
        assert "step3".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState();

        statefulNode = statefulService.stepBack(chainId, context); //使用实例id
        assert "step2".equals(statefulNode.getNode().getId());
        assert StateType.WAITING == statefulNode.getState();
    }

    private void assertNode(StatefulTask node, String id) {
        assert node.getState() == StateType.WAITING;
        assert id.equals(node.getNode().getId());
    }
}