package demo.flow.stateful;

import org.noear.solon.flow.Chain;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.NodeDecl;
import org.noear.solon.flow.stateful.FlowStatefulService;
import org.noear.solon.flow.stateful.Operation;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.flow.stateful.controller.ActorStateController;
import org.noear.solon.flow.stateful.driver.StatefulSimpleFlowDriver;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

/**
 * @author noear 2025/3/28 created
 */
public class OaActionDemo {
    FlowEngine flowEngine =  FlowEngine.newInstance(StatefulSimpleFlowDriver.builder()
            .stateController(new ActorStateController())
            .stateRepository(new InMemoryStateRepository())
            .build());

    FlowStatefulService statefulService = flowEngine.statefulService();

    String instanceId = "guid1";
    String chainId = "f1";

    //审批
    public void case1() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        context.put("actor", "A");
        StatefulTask task = statefulService.getTask(chainId, context);

        //展示界面，操作。然后：

        context.put("op", "审批");//作为状态的一部分
        statefulService.postOperation(context, task.getNode(), Operation.FORWARD);
    }

    //回退
    public void case2() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        context.put("actor", "A");
        StatefulTask task = statefulService.getTask(chainId, context);

        context.put("op", "回退");//作为状态的一部分
        statefulService.postOperation(context, task.getNode(), Operation.BACK);
    }

    //任意跳转（通过）
    public void case3_1() throws Exception {
        FlowContext context = new FlowContext(instanceId);

        String nodeId = "demo1";

        while (true) {
            StatefulTask task = statefulService.getTask(chainId, context);
            context.put("op", "任意转跳");//作为状态的一部分
            statefulService.postOperation(context, task.getNode(), Operation.FORWARD);

            //到目标节点了
            if (task.getNode().getId().equals(nodeId)) {
                break;
            }
        }
    }

    //任意跳转（退回）
    public void case3_2() throws Exception {
        FlowContext context = new FlowContext(instanceId);

        String nodeId = "demo1"; //实际可能需要遍历节点树，并检查各节点状态；再回退

        while (true) {
            StatefulTask statefulNode = statefulService.getTask(chainId, context);
            context.put("op", "任意转跳");//作为状态的一部分
            statefulService.postOperation(context, statefulNode.getNode(), Operation.BACK);

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
        StatefulTask task = statefulService.getTask(chainId, context);

        context.put("op", "委派");//作为状态的一部分
        statefulService.postOperation(context, task.getNode(), Operation.FORWARD);
    }

    //转办（与委派技术实现差不多）
    public void case5() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        context.put("actor", "A");
        context.put("transfer", "B"); //需要定制下状态操作员（用A检测，但留下B的状态记录）
        StatefulTask task = statefulService.getTask(chainId, context);

        context.put("op", "转办");//作为状态的一部分
        statefulService.postOperation(context, task.getNode(), Operation.FORWARD);
    }

    //催办
    public void case6() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        StatefulTask task = statefulService.getTask(chainId, context);

        String actor = task.getNode().getMeta("actor");
        //发邮件（或通知）
    }

    //取回（技术上与回退差不多）
    public void case7() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        StatefulTask task = statefulService.getTask(chainId, context);

        //回退到顶（给发起人）；相当于重新开始走流程
        context.put("op", "取回");//作为状态的一部分
        statefulService.postOperation(context, task.getNode(), Operation.RESTART);
    }

    //撤销（和回退没啥区别）
    public void case8() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        StatefulTask task = statefulService.getTask(chainId, context);

        context.put("op", "撤销");//作为状态的一部分
        statefulService.postOperation(context, task.getNode(), Operation.BACK);
    }

    //中止
    public void case9() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        StatefulTask task = statefulService.getTask(chainId, context);

        context.put("op", "中止");//作为状态的一部分
        statefulService.postOperation(context, task.getNode(), Operation.TERMINATED);
    }

    //抄送
    public void case10() throws Exception {
        FlowContext context = new FlowContext(instanceId);
        StatefulTask node = statefulService.getTask(chainId, context);

        statefulService.postOperation(context, node.getNode(), Operation.FORWARD);
        //提交后，会自动触发任务（如果有抄送配置，自动执行）
    }

    //加签
    public void case11() throws Exception {
        String gatewayId = "g1";
        Chain chain = Chain.parseByText(flowEngine.getChain(chainId).toJson()); //复制
        //添加节点
        chain.addNode(NodeDecl.activityOf("a3").linkAdd("b2"));
        //替代旧的网关（加上 a3 节点）
        chain.addNode(NodeDecl.parallelOf(gatewayId).linkAdd("a1").linkAdd("a2").linkAdd("a3"));

        //把新的链配置，做为实例对应的流配置
    }

    //减签
    public void case12() throws Exception {
        //通过状态操作员和驱动定制，让某个节点不需要处理
        FlowContext context = new FlowContext(instanceId);
        StatefulTask node = statefulService.getTask(chainId, context);

        statefulService.postOperation(context, node.getNode(), Operation.FORWARD);
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
        //配置时，使用并行网关 //驱动定制时，如果元数据申明是或签：一个分支“完成”，另一分支自动为“完成”
    }

    //暂存
    public void case17() throws Exception {
        //不提交操作即可
    }
}