package demo.flow.stateful;

import org.noear.solon.flow.*;
import org.noear.solon.flow.stateful.FlowStatefulService;
import org.noear.solon.flow.stateful.Operation;
import org.noear.solon.flow.stateful.StatefulTask;
import org.noear.solon.flow.stateful.controller.ActorStateController;
import org.noear.solon.flow.stateful.repository.InMemoryStateRepository;

/**
 * @author noear 2025/3/28 created
 */
public class OaActionDemo {
    FlowEngine flowEngine = FlowEngine.newInstance();

    FlowStatefulService statefulService = flowEngine.forStateful();
    ActorStateController stateController = new ActorStateController();
    InMemoryStateRepository stateRepository = new InMemoryStateRepository();

    String instanceId = "i1"; //审批实例id
    Graph graph;//审批实例对应的流程图

    //审批
    public void case1() throws Exception {
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository);
        context.put("actor", "A");
        StatefulTask task = statefulService.getTask(graph, context);

        //展示界面，操作。然后：

        context.put("op", "审批");//作为状态的一部分
        statefulService.postOperation(task.getNode(), Operation.FORWARD, context);
    }

    //回退
    public void case2() throws Exception {
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository);
        context.put("actor", "A");
        StatefulTask task = statefulService.getTask(graph, context);

        context.put("op", "回退");//作为状态的一部分
        statefulService.postOperation(task.getNode(), Operation.BACK, context);
    }

    //任意跳转（通过）
    public void case3_1() throws Exception {
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository);

        String nodeId = "demo1";

        statefulService.postOperation(graph, nodeId, Operation.FORWARD_JUMP, context);
        StatefulTask task = statefulService.getTask(graph, context);
    }

    //任意跳转（退回）
    public void case3_2() throws Exception {
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository);

        String nodeId = "demo1";

        statefulService.postOperation(graph, nodeId, Operation.BACK_JUMP, context);
        StatefulTask task = statefulService.getTask(graph, context);
    }

    //委派
    public void case4() throws Exception {
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository);
        context.put("actor", "A");
        context.put("delegate", "B"); //需要定制下状态操作员（用A检测，但留下B的状态记录）
        StatefulTask task = statefulService.getTask(graph, context);

        context.put("op", "委派");//作为状态的一部分
        statefulService.postOperation(task.getNode(), Operation.FORWARD, context);
    }

    //转办（与委派技术实现差不多）
    public void case5() throws Exception {
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository);
        context.put("actor", "A");
        context.put("transfer", "B"); //需要定制下状态操作员（用A检测，但留下B的状态记录）
        StatefulTask task = statefulService.getTask(graph, context);

        context.put("op", "转办");//作为状态的一部分
        statefulService.postOperation(task.getNode(), Operation.FORWARD, context);
    }

    //催办
    public void case6() throws Exception {
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository);
        StatefulTask task = statefulService.getTask(graph, context);

        String actor = task.getNode().getMeta("actor");
        //发邮件（或通知）
    }

    //取回（技术上与回退差不多）
    public void case7() throws Exception {
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository);
        StatefulTask task = statefulService.getTask(graph, context);

        //回退到顶（给发起人）；相当于重新开始走流程
        context.put("op", "取回");//作为状态的一部分
        statefulService.postOperation(task.getNode(), Operation.RESTART, context);
    }

    //撤销（和回退没啥区别）
    public void case8() throws Exception {
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository);
        StatefulTask task = statefulService.getTask(graph, context);

        context.put("op", "撤销");//作为状态的一部分
        statefulService.postOperation(task.getNode(), Operation.BACK, context);
    }

    //中止
    public void case9() throws Exception {
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository);
        StatefulTask task = statefulService.getTask(graph, context);

        context.put("op", "中止");//作为状态的一部分
        statefulService.postOperation(task.getNode(), Operation.TERMINATED, context);
    }

    //抄送
    public void case10() throws Exception {
        FlowContext context = FlowContext.of(instanceId, stateController, stateRepository);
        StatefulTask task = statefulService.getTask(graph, context);

        statefulService.postOperation(task.getNode(), Operation.FORWARD, context);
        //提交后，会自动触发任务（如果有抄送配置，自动执行）
    }

    //加签
    public void case11() throws Exception {
        String gatewayId = "g1";
        Graph graphNew = Graph.copy(graph, decl->{
            //添加节点
            decl.addActivity("a3").linkAdd("b2");
            //添加连接（加上 a3 节点）
            decl.getNode(gatewayId).linkAdd("a3");
        }); //复制

        //把新的图配置，做为实例对应的流配置
        graph = graphNew;
        String graphJson = graphNew.toJson();
    }

    //减签
    public void case12() throws Exception {
        String gatewayId = "g1";
        Graph graphNew = Graph.copy(graph, decl->{
            //添加节点
            decl.removeNode("a3");
            //添加连接（加上 a3 节点）
            decl.getNode(gatewayId).linkRemove("a3");
        }); //复制

        //把新的图配置，做为实例对应的流配置
        graph = graphNew;
        String graphJson = graphNew.toJson();
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