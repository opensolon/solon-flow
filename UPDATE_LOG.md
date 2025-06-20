
### 3.4.0

* 调整 solon-flow stateful 相关概念（提交活动状态，改为提交操作）
* 调整 solon-flow StateType 拆分为：StateType 和 StateOperation
* 调整 solon-flow StatefulFlowEngine:postActivityState 更名为 postOperation
* 调整 solon-flow StatefulFlowEngine:postActivityStateIfWaiting 更名为 postOperationIfWaiting


方法名称调整：

| 旧方法                          | 新方法                      |   |
|------------------------------|--------------------------|---|
| `getActivityNodes`           | `getActivitys`           |   |
| `getActivityNode`            | `getActivity`            |   |
|                              |                          |   |
| `postActivityStateIfWaiting` | `postOperationIfWaiting` |   |
| `postActivityState`          | `postOperation`          |   |

状态类型拆解后的对应关系（之前状态与操作混一起，不合理）

| StateType(旧)         | StateType(新)          | StateOperation(新)     |
|----------------------|-----------------------|--------------------|
| `UNKNOWN(0)`         | `UNKNOWN(0)`          | `UNKNOWN(0)`       |
| `WAITING(1001)`      | `WAITING(1001)`       | `BACK(1001)`       |
| `COMPLETED(1002)`    | `COMPLETED(1002)`     | `FORWARD(1002)`    |
| `TERMINATED(1003)`   | `TERMINATED(1003)`    | `TERMINATED(1003)` |
| `RETURNED(1004)`     |                       | `BACK(1001)`       |
| `RESTART(1005)`      |                       | `RESTART(1004)`    |




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