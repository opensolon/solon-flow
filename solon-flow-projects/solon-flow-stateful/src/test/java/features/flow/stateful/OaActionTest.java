package features.flow.stateful;

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
        StatefulFlowContext context = new StatefulFlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postNodeState(context, node.getNode(), NodeStates.PASS);
    }

    //回退
    public void case2() throws Exception {
        StatefulFlowContext context = new StatefulFlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postNodeState(context, node.getNode(), NodeStates.WITHDRAW);
    }

    //任意跳转（通过）
    public void case3_1() throws Exception {
        StatefulFlowContext context = new StatefulFlowContext(instanceId);

        String nodeId = "demo1";
        flowEngine.postNodeState(context, chainId, nodeId, NodeStates.PASS);
    }

    //任意跳转（退回）
    public void case3_2() throws Exception {
        StatefulFlowContext context = new StatefulFlowContext(instanceId);

        String nodeId = "demo1"; //实际可能需要遍历节点树，并检查各节点状态；再回退
        flowEngine.postNodeState(context, chainId, nodeId, NodeStates.WITHDRAW);
    }

    //委派
    public void case4() throws Exception {
        StatefulFlowContext context = new StatefulFlowContext(instanceId);
        context.put("actor", "A");
        context.put("delegate", "B"); //需要定制下状态操作员（用A检测，但留下B的状态记录）
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postNodeState(context, node.getNode(), NodeStates.PASS);
    }

    //转办（与委派技术实现差不多）
    public void case5() throws Exception {
        StatefulFlowContext context = new StatefulFlowContext(instanceId);
        context.put("actor", "A");
        context.put("delegate", "B"); //需要定制下状态操作员（用A检测，但留下B的状态记录）
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postNodeState(context, node.getNode(), NodeStates.PASS);
    }

    //催办
    public void case6() throws Exception {
        StatefulFlowContext context = new StatefulFlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        String actor = node.getNode().getMeta("actor");
        //发邮件
    }

    //取回（技术上与回退差不多）
    public void case7() throws Exception {
        StatefulFlowContext context = new StatefulFlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        //回退到顶（给发起人）；相当于重新开始走流程
        flowEngine.postNodeState(context, node.getNode(), NodeStates.WITHDRAW_ALL);
    }

    //撤销（和回退没啥区别）
    public void case8() throws Exception {
        StatefulFlowContext context = new StatefulFlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postNodeState(context, node.getNode(), NodeStates.WITHDRAW);
    }

    //中止
    public void case9() throws Exception {
        StatefulFlowContext context = new StatefulFlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postNodeState(context, node.getNode(), NodeStates.REJECT);
    }

    //抄送
    public void case10() throws Exception {
        StatefulFlowContext context = new StatefulFlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postNodeState(context, node.getNode(), NodeStates.PASS);
        //提交后，会自动触发任务（如果有抄送配置，自动执行）
    }

    //加签
    public void case11() throws Exception {
        //todo: 暂时不支持
    }

    //减签
    public void case12() throws Exception {
        //通过状态操作员和驱动定制，让某个节点不需要处理
        StatefulFlowContext context = new StatefulFlowContext(instanceId);
        StatefulNode node = flowEngine.getActivityNode(chainId, context);

        flowEngine.postNodeState(context, node.getNode(), NodeStates.PASS);
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
        //todo: 暂时不支持，但可能加一个：“或网关”实现
    }

    //暂存
    public void case17() throws Exception {
        //不提交节点状态即可
    }
}