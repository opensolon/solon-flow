
### 3.5.0

* 添加 solon-flow FlowDriver:postHandleTask 方法
* 调整 solon-flow FlowContext 拆分为：FlowContext（对外） 和 FlowExchanger（对内）
* 调整 solon-flow FlowContext 移除 result 字段（所有数据基于 model 交换）
* 调整 solon-flow 移除 StatefulSimpleFlowDriver 功能合并到 SimpleFlowDriver（简化）
* 调整 solon-flow 新增 stateless 包，明确有状态与无状态这两个概念（StatelessFlowContext 更名为 StatefulFlowContext）
* 调整 solon-flow FlowStatefulService 接口，每个方法的 context 参数移到最后位（保持一致性）
* 调整 solon-flow 新增 StatefulSupporter 接口，方便 FlowContext 完整的状态控制
* 调整 solon-flow StateRepository 接口的方法命名，与 StatefulSupporter 保持一致性

### 3.4.3

* 新增 solon-flow iterator 循环网关（`$for`,`$in`）
* 新增 solon-flow activity 节点流入流出模式（`$imode`,`$omode`），且于二次定制开发
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