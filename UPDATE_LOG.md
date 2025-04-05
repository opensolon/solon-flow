### 3.1.3

* 添加 FlowEngine 直接支持节点执行支持
* 添加 FlowContext:backup,recovery 备份和恢复方法
* 添加 NodeDecl 快捷构建器
* 优化 NodeType 编号设计
* 优化 StatefulFlowEngine 添加 FlowEngine 实现
* 修复 StatefulFlowEngine stepBack 当遇到网关时，没有回到上一级的问题

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