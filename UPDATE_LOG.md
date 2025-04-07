### 3.1.3

* 添加 solon-flow FlowEngine 直接支持节点执行支持
* 添加 solon-flow FlowContext:backup,recovery 备份和恢复方法
* 添加 solon-flow NodeDecl 快捷构建器
* 添加 solon-flow AbstractFlowDriver 对 `$xxx` 从链元信息引用脚本的支持
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