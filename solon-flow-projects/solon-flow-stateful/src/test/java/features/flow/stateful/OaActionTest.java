package features.flow.stateful;

import org.noear.solon.flow.*;
import org.noear.solon.flow.stateful.*;

/**
 * @author noear 2025/3/28 created
 */
public class OaActionTest {
    StatefulFlowEngine flowEngine = new StatefulFlowEngine(StatefulSimpleFlowDriver.builder().build());

    String instanceId = "guid1";
    String chainId = "f1";

    //审批
    public void case1() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postActivityState(context, node.getNode(), NodeState.COMPLETED);
    }

    //回退
    public void case2() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postActivityState(context, node.getNode(), NodeState.RETURNED);
    }

    //任意跳转（通过）
    public void case3_1() throws Exception {
        FlowContext context = new FlowContext(instanceId);

        String nodeId = "demo1";

        while (true) {
            StatefulNode statefulNode = flowEngine.getActivityNode(chainId, context);
            flowEngine.postActivityState(context, statefulNode.getNode(), NodeState.SKIP);

            //到目标节点了
            if(statefulNode.getNode().getId().equals(nodeId)) {
                break;
            }
        }
    }

    //任意跳转（退回）
    public void case3_2() throws Exception {
        FlowContext context = new FlowContext(instanceId);

        String nodeId = "demo1"; //实际可能需要遍历节点树，并检查各节点状态；再回退

        while (true) {
            StatefulNode statefulNode = flowEngine.getActivityNode(chainId, context);
            flowEngine.postActivityState(context, statefulNode.getNode(), NodeState.RETURNED);

            //到目标节点了
            if (statefulNode.getNode().getId().equals(nodeId)) {
                break;
            }
        }
    }

    //委派
    public void case4() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        context.put("actor", "A");
        context.put("delegate", "B"); //需要定制下状态操作员（用A检测，但留下B的状态记录）
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postActivityState(context, node.getNode(), NodeState.COMPLETED);
    }

    //转办（与委派技术实现差不多）
    public void case5() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        context.put("actor", "A");
        context.put("delegate", "B"); //需要定制下状态操作员（用A检测，但留下B的状态记录）
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postActivityState(context, node.getNode(), NodeState.COMPLETED);
    }

    //催办
    public void case6() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        String actor = node.getNode().getMeta("actor");
        //发邮件
    }

    //取回（技术上与回退差不多）
    public void case7() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        //回退到顶（给发起人）；相当于重新开始走流程
        flowEngine.postActivityState(context, node.getNode(), NodeState.RESTART);
    }

    //撤销（和回退没啥区别）
    public void case8() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postActivityState(context, node.getNode(), NodeState.RETURNED);
    }

    //中止
    public void case9() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postActivityState(context, node.getNode(), NodeState.TERMINATED);
    }

    //抄送
    public void case10() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postActivityState(context, node.getNode(), NodeState.COMPLETED);
        //提交后，会自动触发任务（如果有抄送配置，自动执行）
    }

    //加签
    public void case11() throws Exception {
        String gatewayId= "g1";
        Chain chain = Chain.parseByText(flowEngine.getChain(chainId).toJson()); //复制
        //添加节点
        chain.addNode(new NodeDecl("a3", NodeType.ACTIVITY).linkAdd("b2"));
        //替代旧的网关（加上 a3 节点）
        chain.addNode(new NodeDecl(gatewayId, NodeType.PARALLEL).linkAdd("a1").linkAdd("a2").linkAdd("a3"));

        //把新的链配置，做为实例对应的流配置
    }

    //减签
    public void case12() throws Exception {
        //通过状态操作员和驱动定制，让某个节点不需要处理
        FlowContext context = new FlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postActivityState(context, node.getNode(), NodeState.COMPLETED);
    }

    //会签
    public void case13() throws Exception {
        //配置时，使用并行网关
    }

    //票签
    public void case15() throws Exception {
        //配置时，使用并行网关（收集投票）；加一个排他网关（判断票数）
    }

    //或签
    public void case16() throws Exception {
        //配置时，使用并行网关 //驱动定制时，如果元数据申明是或签：一个分支“完成”，另一分支自动为“跳过”
    }

    //暂存
    public void case17() throws Exception {
        //不提交节点状态即可
    }
}