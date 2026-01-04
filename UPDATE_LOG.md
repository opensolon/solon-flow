### 待议

* 优化 solon-flow （有包含结构的）网关的流入流出架构，使不再需要记录栈和计数器???


### 3.8.1

* 添加 `solon-flow` FlowContext:toJson,fromJson 序列化方法（方便持久化和恢复）
* 添加 `solon-flow` NodeTrace 类
* 添加 `solon-flow` NodeSpec.then 方法
* 添加 `solon-flow` FlowContext.with 方法（强调方法域内的变量）
* 添加 `solon-flow` FlowContext.containsKey 方法
* 添加 `solon-flow` FlowContext.isStopped 方法（用于外部检测）
* 添加 `solon-flow` NamedTaskComponent 接口，方便智能体开发
* 添加 `solon-flow` 多图多引擎状态记录与序列化支持
* 优化 `solon-flow` FlowContext 接口设计，并增加持久化辅助方法
* 优化 `solon-flow` FlowContext.eventBus 内部实现改为字段模式
* 优化 `solon-flow` start 类型节点改为自由流出像 activity 一样（只是没有任务）
* 优化 `solon-flow` 引擎的 onNodeEnd 执行时机（改为任务执行之后，连接流出之前）
* 优化 `solon-flow` 引擎的 onNodeStart 执行时机（改为任务执行之前，连接流入之后）
* 优化 `solon-flow` 在 onNodeEnd 时添加 FlowContext.lastNode 记录（跨流执行后再次恢复）
* 优化 `solon-flow` 引擎的 reverting 处理（支持跨引擎多图场景）
* 优化 `solon-flow` Node,Link toString 处理（加 whenComponent）
* 调整 `solon-flow` FlowContext:executor 转移到 FlowDriver
* 调整 `solon-flow` FlowInterceptor:doIntercept 更名为 doFlowInterceptor，并标为 default（扩展时语义清晰，且不需要强制实现）
* 调整 `solon-flow` NodeTrace 更名为 NodeRecord，并增加 FlowTrace 类。支持跨图多引擎场景
* 移除 `solon-flow` FlowContext:incrAdd,incrGet 弃用预览接口
* 修复 `solon-flow` FlowContext 跨多引擎中转时 exchanger 的冲突问题

兼容变化对照表：

| 旧名称                         | 新名称                    | 备注          |
|-----------------------------|------------------------|-------------|
| FlowInterceptor:doIntercept | doFlowIntercept        | 扩展时语义清晰     |
| FlowContext:executor        | FlowDriver:getExecutor | 上下文不适合配置线程池 |
| FlowContext:incrAdd,incrGet | /                      | 移除          |
| NodeTrace                   | NodeRecord             | 支持跨图多引擎场景          |
| /                           | FlowTrace              | 支持跨图多引擎场景          |


新特性预览：上下文序列化与持久化


```java
//恢复的上下文
FlowContext context = FlowContext.fromJson(json);
//新上下文
FlowContext context = FlowContext.of();

//从恢复上下文开始持行
flowEngine.eval(graph, context);

//转为 json（方便持久化）
json = context.toJson();
```


### 3.8.0

重要变化：

* 第六次预览
* 取消旧的“有状态”、“无状态”概念。
* 新概念分为：通用流程，工作审批流程
* solon-flow 回归通用流程引擎（分离之前的“有状态”概念）。
* 新增 solon-flow-workflow 为工作流性质的封装（未来可能会有 dataflow 等）。


具体更新：

* 插件 `solon-flow` 第六次预览
* 新增 `solon-flow-workflow` 插件（替代 FlowStatefulService）
* 添加 `solon-flow` FlowContext:lastNode() 方法（最后一个运行的节点）
* 添加 `solon-flow` FlowContext:lastNodeId() 方法（最后一个运行的节点Id）
* 添加 `solon-flow` Node.getMetaAs, Link.getMetaAs 方法
* 添加 `solon-flow` NodeSpec:linkRemove 方法（增强修改能力）
* 添加 `solon-flow` Graph:create(id,title,consumer) 方法
* 添加 `solon-flow` Graph:copy(graph,consumer) 方法（方便复制后修改）
* 添加 `solon-flow` GraphSpec:getNode(id) 方法
* 添加 `solon-flow` GraphSpec:addLoop(id) 方法替代 addLooping（后者标为弃用）
* 添加 `solon-flow` FlowEngine:eval(Graph, ..) 系列方法
* 优化 `solon-flow` FlowEngine:eval(Node startNode) 处理，改为从 root 开始恢复到 start 再开始执行（恢复过程中，不会执行任务）
* 调整 `solon-flow` 移除 Activity 节点预览属性 "$imode" 和 "$omode" 
* 调整 `solon-flow` Activity 节点流出改为自由模式（可以多线流出：无条件直接流出，有条件检测后流出）
* 调整 `solon-flow` Node.getMeta 方法返回改为 Object 类型（并新增 getMetaAs）
* 调整 `solon-flow` Evaluation:runTest 改为 runCondition
* 调整 `solon-flow` FlowContext:incrAdd,incrGet 标为弃用（上下文数据为型只能由输入侧决定）
* 调整 `solon-flow` Condition 更名为 ConditionDesc
* 调整 `solon-flow` Task 更名为 ConditionDesc
* 调整 `solon-flow` GraphDecl 重命名改为 GraphSpec，NodeDecl 重命名改为 NodeSpec，LinkDecl 重命名改为 LinkSpec
* 调整 `solon-flow` GraphSpec.parseByText 重命名改为 fromText，parseByUri 重命名改为 fromUri
* 调整 `solon-flow` Graph.parseByText 重命名改为 fromText，parseByUri 重命名改为 fromUri


兼容变化对照表：

| 旧名称                    | 新名称                   | 说明                    |  
|------------------------|-----------------------|-----------------------|
| `GraphDecl`            | `GraphSpec`           | 图定义                   |
| `GraphDecl.parseByXxx` | `GraphSpec.fromXxx`   | 图定义加载                    |
| `Graph.parseByXxx`     | `Graph.fromXxx`       | 图加载                   |
| `LinkDecl`             | `LinkSpec`            | 连接定义                  |
| `NodeDecl`             | `NodeSpec`            | 节点定义                  |
| `Condition`            | `ConditionDesc`       | 条件描述                  |
| `Task`                 | `TaskDesc`            | 任务描述（避免与 workflow 的概念冲突） |
|                        |                       |                       |
| `FlowStatefulService`  | `WorkflowService`     | 工作流服务                 |
| `StatefulTask`         | `Task`                | 任务                    | 
| `Operation`            | `TaskAction`          | 任动工作                  | 
| `TaskType`             | `TaskState`           | 任务状态                  | 


FlowStatefulService 到 WorkflowService 的接口变化对照表：

| 旧名称                          | 新名称                     | 说明     |  
|------------------------------|-------------------------|--------|
| `postOperation(..)`          | `postTask(..)`          | 提交任务   |
| `postOperationIfWaiting(..)` | `postTaskIfWaiting(..)` | 提交任务   |
|                              |                         |        |
| `evel(..)`                   | /                       | 执行     |
| `stepForward(..)`            | /                       | 单步前进   |
| `stepBack(..)`               | /                       | 单步后退   |
|                              |                         |        |
| /                            | `getState(..)`          | 获取状态   |



新特性预览：Graph 硬编码方式（及修改能力增强）

```java
//硬编码
Graph graph = Graph.create("demo1", "示例", spec -> {
    spec.addStart("start").title("开始").linkAdd("01");
    spec.addActivity("n1").task("@AaMetaProcessCom").linkAdd("end");
    spec.addEnd("end").title("结束");
});

//修改
Graph graphNew = Graph.copy(graph, spec -> {
    spec.getNode("n1").linkRemove("end").linkAdd("n2"); //移掉 n1 连接；改为 n2 连接
    spec.addActivity("n2").linkAdd("end");
});
```

新特性预览：FlowContext:lastNodeId （计算的中断与恢复）。参考：https://solon.noear.org/article/1246

```java
flowEngine.eval(graph, context.lastNodeId(), context);
//...（从上一个节点开始执行）
flowEngine.eval(graph, context.lastNodeId(), context);
```


新特性预览：WorkflowService（替代 FlowStatefulService）

```java
WorkflowService workflow = WorkflowService.of(engine, WorkflowDriver.builder()
        .stateController(new ActorStateController()) 
        .stateRepository(new InMemoryStateRepository()) 
        .build());


//1. 取出任务
Task task = workflow.getTask(graph, context);

//2. 提交任务
workflow.postTask(task.getNode(), TaskAction.FORWARD, context);
```

### 3.7.4

* 添加 `solon-flow` Graph:create(id,title,consumer) 方法
* 添加 `solon-flow` GraphDecl:addLoop 方法替代 addLooping（后者标为弃用）
* 添加 `solon-flow` Evaluation:runCondition 方法替代 runTest（后者标为弃用）
* 添加 `solon-flow` FlowContext:lastNode 方法（最后一个运行的节点）
* 添加 `solon-flow` Graph:copy 方法
* 添加 `solon-flow` GraphDecl:getNode 方法
* 添加 `solon-flow` Graph:toYaml(FlowContext)，Graph:toJson(FlowContext) 方法，可输出节点状态（方便前端展示进度）
* 优化 `solon-flow` eval(Node startNode) 处理，改为从 root 开始恢复到 start 再开始执行（恢复过程中，不会执行任务）
* 优化 `solon-flow` stateful 允许 stateController 独立使用（即可以没有 stateRepository）
* 调整 `solon-flow` FlowStatefulService:evel、stepForward、stepBack 标为弃用

### 3.7.3

* 插件 `solon-flow` 第五次预览
* 添加 `solon-flow` Node:task 硬编码能力（直接设置 TaskComponent），方便全动态场景
* 添加 `solon-flow` Node:when 硬编码能力（直接设置 ConditionComponent），方便全动态场景
* 添加 `solon-flow` Link:when 硬编码能力（直接设置 ConditionComponent），方便全动态场景
* 添加 `solon-flow` StateResult ，在计算方面比 StatefulTask 更适合语义
* 添加 `solon-flow` FlowContext:stop(),interrupt() 方法
* 添加 `solon-flow` Graph 快捷创建方法
* 添加 `solon-flow` FlowStatefulService:eval 方法
* 调整 `solon-flow` “链”概念改为“图”（更符合实际结构）
* 调整 `solon-flow` Chain 更名为 Graph，ChainDecl 更名为 GraphDecl
* 调整 `solon-flow` ChainInterceptor,ChainInvocation 更名为 FlowInterceptor,FlowInvocation
* 调整 `solon-flow` 包容网关逻辑，分支空条件为 true，且取消默认概念（之前为：空条件为 false ，且为默认）

solon-flow 兼容说明：

```
现有应用如果没有用 ChainDecl 动态构建，不会受影响。。。如果有？需要换个类名。
```

solon-flow 硬编码更简便：

```java
Graph graph = Graph.create("demo1", spec -> {
    spec.addActivity("n1").task(new Draft()).linkAdd("n2");
    spec.addActivity("n2").task(new Review()).linkAdd("n3");
    spec.addActivity("n3").task(new Confirm());
});
```

### 3.7.2

* dami2 升为 2.0.4

### 3.6.5

* dami2 升为 2.0.4

### 3.6.1

* 添加 `solon-flow` FlowEngine:forStateful，statefulService 标为弃用
* 调整 `solon-flow` 增加 `loop` 类型替代 `iterator`（iterator 增加弃用提醒），并提供更多功能
* 调整 `solon-flow` 所有网关节点增加 `task` 支持，不再需要 `$imode` 和 `$omode`。更适合前端连线控制
* 调整 `solon-flow` 节点属性 `$imode` 和 `$omode` 标为弃用


```yaml
{type: 'loop',meta: {'$for': 'item','$in': [1,3,4]}}
```

### 3.6.0

* dami 升为 2.0.0
* 添加 solon-flow Node:getMetaAsString, getMetaAsNumber, getMetaAsBool 方法

### 3.5.0


本次更新，主要统一了“无状态”、“有状态”流程的基础：引擎、驱动。通过上下文来识别是否为有状态及相关支持。

FlowContext 改为接口，增加了两个重要的方法：

```java
boolean isStateful();
StatefulSupporter statefulSupporter();
```

且，FlowContext 做了分离。解决了，之前在实例范围内不可复用的问题。


#### 兼容说明

* stateful 相关概念与接口有调整
* FlowContext 改为接口，并移除 result 字段（所有数据基于 model 交换）
* FlowContext 内置实现分为：StatelessFlowContext 和 StatefulFlowContext。通过 `FlowContext.of(...)` 实例化。（也可按需定制）
* StateRepository 接口的方法命名调整，与 StatefulSupporter 保持一致性

升级请做好调整与测试。

#### 具体更新


* 添加 solon-flow FlowDriver:postHandleTask 方法
* 添加 solon-flow FlowContext:exchanger 方法（可获取 FlowExchanger 实例）
* 调整 solon-flow FlowContext 拆分为：FlowContext（对外） 和 FlowExchanger（对内）
* 调整 solon-flow FlowContext 移除 result 字段（所有数据基于 model 交换）
* 调整 solon-flow FlowContext get 改为返回 Object（之前为 T），新增 getAs 返回 T（解决 get 不能直接打印的问题）
* 调整 solon-flow 移除 StatefulSimpleFlowDriver 功能合并到 SimpleFlowDriver（简化）
* 调整 solon-flow 新增 stateless 包，明确 “有状态” 与 “无状态” 这两个概念（StatelessFlowContext 和 StatefulFlowContext）
* 调整 solon-flow FlowStatefulService 接口，每个方法的 context 参数移到最后位（保持一致性）
* 调整 solon-flow 新增 StatefulSupporter 接口，方便 FlowContext 完整的状态控制
* 调整 solon-flow StateRepository 接口的方法命名，与 StatefulSupporter 保持一致性
* 调整 solon-flow Chain 拆分为：Chain 和 ChainDecl

两对拆分类的定位：

* FlowContext 侧重对外，可复用（用于传参、策略，状态）
* FlowExchanger 侧重对内，不可复用（用于控制、中间临时状态或变量）
* Chain 为运行态（不可修改）
* ChainDecl 为声明或配置态（可以随时修改）


应用示例：

```java
//FlowContext 构建
FlowContext context = FlowContext.of(); //无状态的
FlowContext context = FlowContext.of("1", stateController); //有状态控制的
FlowContext context = FlowContext.of("1", stateController, stateRepository); //有状态控制的和状态持久化的


//Chain 手动声明
Chain chain = new ChainDecl("d3", "风控计算").create(spec->{
            spec.addNode(NodeDecl.startOf("s").linkAdd("n2"));
            spec.addNode(NodeDecl.activityOf("n1").title("基本信息评分").linkAdd("g1").task("@base_score"));
            spec.addNode(NodeDecl.exclusiveOf("g1").title("分流")
                    .linkAdd("e", l -> l.title("优质用户（评分90以上）").condition("score > 90"))
                    .linkAdd("n2", l -> l.title("普通用户")) //没条件时，做为默认
            );
            spec.addNode(NodeDecl.activityOf("n2").title("电商消费评分").linkAdd("n3").task("@ec_score"));
            spec.addNode(NodeDecl.activityOf("n3").title("黑名单检测").linkAdd("e").task("@bl_score"));
            spec.addNode(NodeDecl.endOf("e").task("."));
        });
```

### 3.4.3

* 新增 solon-flow iterator 循环网关（`$for`,`$in`）
* 新增 solon-flow activity 节点流入流出模式（`$imode`,`$omode`），用于二次定制开发
* 添加 solon-flow ChainInterceptor:onNodeStart, onNodeEnd 方法（扩展拦截的能力）
* 添加 solon-flow 操作：Operation.BACK_JUMP, FORWARD_JUMP

### 3.4.1

* 添加 solon-flow FlowContext:incrGet, incrAdd
* 添加 solon-flow aot 配置
* 优化 solon-flow Chain:parseByDom 节点解析后的添加顺序
* 优化 solon-flow Chain 解析统改为 Yaml 处理，并添加 toYaml 方法
* 优化 solon-flow Chain:toJson 输出（压缩大小，去掉空输出）

### 3.4.0

* 调整 solon-flow stateful 相关概念（提交活动状态，改为提交操作）
* 调整 solon-flow StateType 拆分为：StateType 和 Operation
* 调整 solon-flow StatefulFlowEngine:postActivityState 更名为 postOperation
* 调整 solon-flow StatefulFlowEngine:postActivityStateIfWaiting 更名为 postOperationIfWaiting
* 调整 solon-flow StatefulFlowEngine:getActivity 更名为 getTask
* 调整 solon-flow StatefulFlowEngine:getActivitys 更名为 getTasks
* 调整 solon-flow StatefulFlowEngine 更名为 FlowStatefulService（确保引擎的单一性）
* 添加 solon-flow FlowStatefulService 接口，替换 StatefulFlowEngine（确保引擎的单一性）
* 添加 solon-flow `FlowEngine:statefulService()` 方法
* 添加 solon-flow `FlowEngine:getDriverAs()` 方法


方法名称调整：

| 旧方法                          | 新方法                      |   |
|------------------------------|--------------------------|---|
| `getActivityNodes`           | `getTasks`               |   |
| `getActivityNode`            | `getTask`                |   |
|                              |                          |   |
| `postActivityStateIfWaiting` | `postOperationIfWaiting` |   |
| `postActivityState`          | `postOperation`          |   |

状态类型拆解后的对应关系（之前状态与操作混一起，不合理）

| StateType(旧)         | StateType(新)          | Operation(新)     |
|----------------------|-----------------------|------------------|
| `UNKNOWN(0)`         | `UNKNOWN(0)`          | `UNKNOWN(0)`     |
| `WAITING(1001)`      | `WAITING(1001)`       | `BACK(1001)`     |
| `COMPLETED(1002)`    | `COMPLETED(1002)`     | `FORWARD(1002)`  |
| `TERMINATED(1003)`   | `TERMINATED(1003)`    | `TERMINATED(1003)` |
| `RETURNED(1004)`     |                       | `BACK(1001)`     |
| `RESTART(1005)`      |                       | `RESTART(1004)`  |




### 3.3.3

* 优化 solon-flow FlowContext 变量的线程可见
* 添加 solon-flow parallel 网关多线程并行支持（通过 context.executor 决定）
* 添加 solon-flow LinkDecl:when 方法用于替代 :condition（后者标为弃用）
* 添加 solon-flow parallel 网关多线程并行支持（通过 context.executor 决定）
* 调整 solon-flow FlowDriver:handleTest 更名为 handleCondition （跟 handleTask 容易混）

### 3.3.2

* 优化 solon-flow-designer
* 添加 solon-flow FlowContext:runScript 替代 run（旧名，标为弃用）
* 添加 solon-flow FlowContext:runTask(node, description)方法
* 添加 solon-flow link 支持 when 统一条件（替代 condition）
* 添加 solon-flow activity 多分支流出时支持（逻辑与排他网关相同）
* 添加 solon-flow Counter:incr(key, delta) 方法
* 调整 solon-flow 取消 `type: "@Com"` 的快捷配置模式（示例调整）

### 3.3.1

* 新增 solon-flow-designer (设计器)

### 3.2.0

* 添加 solon-flow FlowEngine 直接支持节点执行支持
* 添加 solon-flow FlowContext:backup,recovery 备份和恢复方法
* 添加 solon-flow NodeDecl 快捷构建器
* 添加 solon-flow AbstractFlowDriver 对 `$xxx` 从链元数据引用脚本的支持
* 添加 solon-flow FlowContext:computeIfAbsent 方法
* 添加 StatefulFlowEngine:getActivityNodes （获取多个活动节点）方法
* 添加 StatefulFlowEngine:postActivityStateIfWaiting 提交活动状态（如果当前节点为等待介入）
* 优化 solon-flow NodeType 代码编号设计
* 优化 solon-flow-stateful StatefulFlowEngine 添加 FlowEngine 实现
* 优化 solon-flow-stateful StateRepository 设计，取消 StateRecord （太业务了，状态记录完全交给应用侧处理）
* 修复 solon-flow-stateful StatefulFlowEngine stepBack 当遇到网关时，没有回到上一级的问题
* 修复 solon-flow-stateful NodeState.RESTART 代码编号的错标问题
* 修复 solon-flow-stateful StatefulSimpleFlowDriver 有状态执行时，任务可能会重复执行的问题
* 调整 solon-flow-stateful NodeState 改为 enum 类型（约束性更强，int 约束太弱了）
* 调整 solon-flow-stateful NodeState 更名为 StateType （更中性些；状态不一定与节点有关，有时像指令）
* 调整 solon-flow-stateful StateOperator 更名为 StateController （意为状态控制器）
* 调整 solon-flow-stateful StatefulFlowEngine 拆分为接口与实现
* 调整 solon-flow-stateful MetaStateController 更名为 ActorStateController 更表意
* 调整 solon-flow-stateful 代码合并到 solon-flow（变成一个项目）

### 3.1.2

* 新增 solon-flow-stateful 插件
* 新增 solon-flow-eval-aviator 插件
* 新增 solon-flow-eval-beetl 插件
* 新增 solon-flow-eval-magic 插件
* 添加 solon-flow Evaluation 接口，为脚本执行解耦
* 添加 solon-flow Container 接口，为组件获取解耦
* 调整 solon-flow ChainContext 更名为 FlowContext (之前的名字容易误解)
* 调整 solon-flow ChainDriver 更名为 FlowDriver (之前的名字容易误解)
* 优化 solon-flow 引擎内部改为回调的机制???