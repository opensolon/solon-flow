

# solon-flow-workflow

Action 效果说明：

| 动作 (Action)  | 中间节点处理 (A, B) | 目标节点 C 的最终状态 | 流程停留在哪里？ | 业务语义               |
|--------------|---------------|--------------|----------|--------------------|
| FORWARD      | /             | COMPLETED    | C 的下一步   | 正常办理。              |
| FORWARD_JUMP | 标记为 COMPLETED | WAITING      | 停在 C     | 跨级指派：跳过中间，指派 C 办理。 |
| BACK         | /             | REMOVED(无状态)      | C 的前一步   | 常规退回。              |
| BACK_JUMP    | 状态被 REMOVED   | WAITING      | 停在 C     | 指定驳回：撤销中间，要求 C 重办。 |
| RESTART      | 全部 REMOVED    | REMOVED      | 流程起点     | 清空所有状态，回到初始位置。     |
| TERMINATE      | /             | TERMINATE      | 停在 C      | 之后，流程不能再前进。        |

WorkflowExecutor 方法说明：


| 方法名           | 核心行为      | 副作用        | 业务语义                                                                           |
|---------------|-----------|------------|--------------------------------------------------------------------------------|
| claimTask     | 权限匹配+激活   | 写入 WAITING | 认领：如果节点是可操作的且状态是 UNKNOWN 或 WAITING 则认领成功，该节点在 StateRepository 中会变为 WAITING 状态。 |
| findTask      | 逻辑探测      | /          | 查询：如果节点状态是 UNKNOWN 或 WAITING 或 TERMINATE 则查找成功（或者返回最后一个节点。BACK_JUMP 时会用到）      |
| findNextTasks | 全量路径探测    |            | 查询下一步：多分支查询，如果节点状态是 UNKNOWN 或 WAITING。                                         |
| getState      | 快照查询      | /          | 获取指定节点在 StateRepository 中的当前状态。                                                |



轻量级审批型工作流引擎


示例:

```java
public class Demo {
    public void demo(FlowEngine engine, Graph graph) {
        //0. 初始化工作流
        workflowExecutor workflow = workflowExecutor.of(engine, WorkflowDriver.builder()
                .stateController(new ActorStateController())
                .stateRepository(new InMemoryStateRepository())
                .build());

        FlowContext context = FlowContext.of("case1");

        //1. 匹配当前任务
        Task current = workflow.matchTask(graph, context);

        //2. 提交任务
        workflow.submitTask(current.getNode(), TaskAction.FORWARD, context);
    }
}
```


## AO 常见业务实现参考


```java
public class OaActionDemo {
    WorkflowExecutor workflow = WorkflowExecutor.of(FlowEngine.newInstance(),
            new ActorStateController(),
            new InMemoryStateRepository());

    String instanceId = "i1"; //审批实例id

    //获取实例对应的流程图
    public Graph getGraph(String instanceId) {
        String graphJson = ""; //从持久层查询
        return Graph.fromText(graphJson);
    }

    //更新实例对应的流程图
    public void setGraph(String instanceId, Graph graph) {
        String graphJson = graph.toJson();
        //更新到持久层
    }

    //审批
    public void case1() throws Exception {
        Graph graph = getGraph(instanceId);

        FlowContext context = FlowContext.of(instanceId);
        context.put("actor", "A");
        Task current = workflow.matchTask(graph, context);

        //展示界面，操作。然后：

        context.put("op", "审批");//作为状态的一部分
        workflow.submitTask(graph, current.getNodeId(), TaskAction.FORWARD, context);
    }

    //回退
    public void case2() throws Exception {
        Graph graph = getGraph(instanceId);

        FlowContext context = FlowContext.of(instanceId);
        context.put("actor", "A");
        Task current = workflow.matchTask(graph, context);

        context.put("op", "回退");//作为状态的一部分
        workflow.submitTask(graph, current.getNodeId(), TaskAction.BACK, context);
    }

    //任意跳转（通过）
    public void case3_1() throws Exception {
        Graph graph = getGraph(instanceId);

        FlowContext context = FlowContext.of(instanceId);

        String nodeId = "demo1";

        workflow.submitTask(graph, nodeId, TaskAction.FORWARD_JUMP, context);
        Task current = workflow.matchTask(graph, context);
    }

    //任意跳转（退回）
    public void case3_2() throws Exception {
        Graph graph = getGraph(instanceId);

        FlowContext context = FlowContext.of(instanceId);

        String nodeId = "demo1";

        workflow.submitTask(graph, nodeId, TaskAction.BACK_JUMP, context);
        Task current = workflow.matchTask(graph, context);
    }

    //委派
    public void case4() throws Exception {
        Graph graph = getGraph(instanceId);

        FlowContext context = FlowContext.of(instanceId);
        context.put("actor", "A");
        context.put("delegate", "B"); //需要定制下状态操作员（用A检测，但留下B的状态记录）
        Task current = workflow.matchTask(graph, context);

        context.put("op", "委派");//作为状态的一部分
        workflow.submitTask(graph, current.getNodeId(), TaskAction.FORWARD, context);
    }

    //转办（与委派技术实现差不多）
    public void case5() throws Exception {
        Graph graph = getGraph(instanceId);

        FlowContext context = FlowContext.of(instanceId);
        context.put("actor", "A");
        context.put("transfer", "B"); //需要定制下状态操作员（用A检测，但留下B的状态记录）
        Task current = workflow.matchTask(graph, context);

        context.put("op", "转办");//作为状态的一部分
        workflow.submitTask(graph, current.getNodeId(), TaskAction.FORWARD, context);
    }

    //催办
    public void case6() throws Exception {
        Graph graph = getGraph(instanceId);

        FlowContext context = FlowContext.of(instanceId);
        Task current = workflow.matchTask(graph, context);

        String actor = current.getNode().getMetaAs("actor");
        //发邮件（或通知）
    }

    //取回（技术上与回退差不多）
    public void case7() throws Exception {
        Graph graph = getGraph(instanceId);

        FlowContext context = FlowContext.of(instanceId);
        Task current = workflow.matchTask(graph, context);

        //回退到顶（给发起人）；相当于重新开始走流程
        context.put("op", "取回");//作为状态的一部分
        workflow.submitTask(graph, current.getNodeId(), TaskAction.RESTART, context);
    }

    //撤销（和回退没啥区别）
    public void case8() throws Exception {
        Graph graph = getGraph(instanceId);

        FlowContext context = FlowContext.of(instanceId);
        Task current = workflow.matchTask(graph, context);

        context.put("op", "撤销");//作为状态的一部分
        workflow.submitTask(graph, current.getNodeId(), TaskAction.BACK, context);
    }

    //中止
    public void case9() throws Exception {
        Graph graph = getGraph(instanceId);

        FlowContext context = FlowContext.of(instanceId);
        Task current = workflow.matchTask(graph, context);

        context.put("op", "中止");//作为状态的一部分
        workflow.submitTask(graph, current.getNodeId(), TaskAction.TERMINATE, context);
    }

    //抄送
    public void case10() throws Exception {
        Graph graph = getGraph(instanceId);

        FlowContext context = FlowContext.of(instanceId);
        Task current = workflow.matchTask(graph, context);

        workflow.submitTask(graph, current.getNodeId(), TaskAction.FORWARD, context);
        //提交后，会自动触发任务（如果有抄送配置，自动执行）
    }

    //加签
    public void case11() throws Exception {
        Graph graph = getGraph(instanceId);

        String gatewayId = "g1";
        Graph graphNew = Graph.copy(graph, spec -> {
            //添加节点
            spec.addActivity("a3").linkAdd("b2");
            //添加连接（加上 a3 节点）
            spec.getNode(gatewayId).linkAdd("a3");
        }); //复制

        //把新图，做为实例对应的流配置
        setGraph(instanceId, graphNew);
    }

    //减签
    public void case12() throws Exception {
        Graph graph = getGraph(instanceId);

        String gatewayId = "g1";
        Graph graphNew = Graph.copy(graph, spec -> {
            //添加节点
            spec.removeNode("a3");
            //添加连接（加上 a3 节点）
            spec.getNode(gatewayId).linkRemove("a3");
        }); //复制

        //把新图，做为实例对应的流配置
        setGraph(instanceId, graphNew);
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
```