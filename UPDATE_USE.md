v3.1.3 后

### 1、solon-flow-stateful 合并到 solon-flow （大小增加 17k）

其它变更概要：

* 包名保持不变
* StatefulFlowEngine 拆分为接口和实现（StatefulFlowEngine, StatefulFlowEngineDefault）
* StatefulSimpleFlowDriver 拆分为接口和实现（StatefulFlowDriver，StatefulSimpleFlowDriver）
* NodeState 变更为 StateType 枚举类型 //原来的状态部分是指令，不一定是节点的状态
* StateOperator 变更为 StateController
* StateRepository 简化，不再提供状态记录，但提供了一个事件 
* StatefulFlowEngine 添加了 getActivityNodes（获取下一步的所有节点）, postActivityStateIfWaiting（提交活动状态时，增加节点与状态检查）
* MetaStateController 变更为 ActorStateController（参与者状态控制器） 更表意


ActorStateController 与 BlockStateController 的区别（可以按需再定制）：

* BlockStateController，每个活动节点都有权操作，默认状态下每个活动都需要“手动前进”
* ActorStateController，根据元数据配置检测是否有权操作，默认状态下无“参与者”配置的会“自动前进”

### 2、插件的默认流引擎实例可以被替换了

使用 StatefulFlowEngine 时（兼容无状态）。以流上下文有没有“实例id”，来区分是：有状态，还是无状态。

* `FlowContext.of()` 无状态
* `FlowContext.of("1")` 有状态 //有实例id


```java
@Configuration
public class DemoConfig {
    //替换掉默认引擎（会自动加载 solon.flow 配置的链资源）
    @Bean
    public StatefulFlowEngine statefulFlowEngine() {
        return StatefulFlowEngine.newInstance(StatefulSimpleFlowDriver.builder()
                .stateController(new ActorStateController("actor"))
                .stateRepository(new InMemoryStateRepository()) //状态仓库（支持持久化）
                .build());
    }

    //可用 StatefulFlowEngine 或 FlowEngine 注入
    @Bean
    public void case1(FlowEngine flowEngine) {
        flowEngine.eval("f1");
    }
    
    @Bean
    public void case2(StatefulFlowEngine flowEngine) {
        //断点控制场景
        flowEngine.stepForward("f2", FlowContext.of("1"));
        flowEngine.stepBack("f2", FlowContext.of("1"));
    }

    @Bean
    public void case3(StatefulFlowEngine flowEngine) {
        //等待介入场景
        StatefulNode statefulNode = flowEngine.getActivityNode("f2", FlowContext.of("1"));
        flowEngine.postActivityState(FlowContext.of("1"), statefulNode.getNode(), StateType.COMPLETED);
    }
}
```

### 3、动态构建链变得简单了些

NodeDecl 添加了一组 xxxOf 的方法，可省去枚举。flowEngine:eval 增加直接的 node 入参方法

```java
@Component
public class DemoCom {
    @Bean
    public void case1(FlowEngine flowEngine){
        Chain chain = new Chain("c1");
        chain.addNode(NodeDecl.activityOf("n1").task("System.out.println(\"hello world!\");"));

        flowEngine.eval(chain.getNode("n1"));
    }
}
```

### 4、AbstractFlowDriver 的脚本处理支持从链元数据里引用

如果任务的脚本太复杂，影响 layout 配置的观感。可以把它移到链的元数据上。然后通过 `$` 符引用链的元数据

```yaml
# demo.chain.yml
id: "c1"
title: "计算编排"
layout:
  - { type: "start"}
  - { task: '$demo.com1'}
  - { task: '$demo.com2'}
  - { type: "end"}
meta:
  demo:
      com1: |
        if(user.id == 0) {
            return;
        }
        System.out.println("ACom");
      com2: |
        System.out.println("BCom");
```

