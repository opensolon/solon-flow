<h1 align="center" style="text-align:center;">
<img src="solon_icon.png" width="128" />
<br />
Solon-Flow
</h1>
<p align="center">
	<strong>Java 通用流程编排框架（采用 yaml 和 json 编排格式）</strong>
    <br/>
    <strong>克制、高效、开放</strong>
</p>
<p align="center">
	<a href="https://solon.noear.org/article/learn-solon-flow">https://solon.noear.org/article/learn-solon-flow</a>
</p>

<p align="center">
    <a href="https://deepwiki.com/opensolon/solon-flow"><img src="https://deepwiki.com/badge.svg" alt="Ask DeepWiki"></a>
    <a target="_blank" href="https://central.sonatype.com/search?q=org.noear%3Asolon-parent">
        <img src="https://img.shields.io/maven-central/v/org.noear/solon.svg?label=Maven%20Central" alt="Maven" />
    </a>
    <a target="_blank" href="LICENSE">
		<img src="https://img.shields.io/:License-Apache2-blue.svg" alt="Apache 2" />
	</a>
    <a target="_blank" href="https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html">
		<img src="https://img.shields.io/badge/JDK-8-green.svg" alt="jdk-8" />
	</a>
    <a target="_blank" href="https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html">
		<img src="https://img.shields.io/badge/JDK-11-green.svg" alt="jdk-11" />
	</a>
    <a target="_blank" href="https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html">
		<img src="https://img.shields.io/badge/JDK-17-green.svg" alt="jdk-17" />
	</a>
    <a target="_blank" href="https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html">
		<img src="https://img.shields.io/badge/JDK-21-green.svg" alt="jdk-21" />
	</a>
    <a target="_blank" href="https://www.oracle.com/java/technologies/downloads/">
		<img src="https://img.shields.io/badge/JDK-25-green.svg" alt="jdk-25" />
	</a>
    <br />
    <a target="_blank" href='https://gitee.com/opensolon/solon-flow/stargazers'>
		<img src='https://gitee.com/opensolon/solon-flow/badge/star.svg' alt='gitee star'/>
	</a>
    <a target="_blank" href='https://github.com/opensolon/solon-flow/stargazers'>
		<img src="https://img.shields.io/github/stars/opensolon/solon-flow.svg?style=flat&logo=github" alt="github star"/>
	</a>
    <a target="_blank" href='https://gitcode.com/opensolon/solon-flow/stargazers'>
		<img src='https://gitcode.com/opensolon/solon-flow/star/badge.svg' alt='gitcode star'/>
	</a>
</p>

<hr />


## 简介

面向全场景的 Java 流程编排框架。是 Solon 项目的一部分。也可嵌入到 SpringBoot、jFinal、Vert.X、Quarkus、Micronaut 等框架中使用。

支持已知流程编排的各种场景:

* 可用于计算（或任务）的编排场景
* 可用于业务规则和决策处理型的编排场景
* 可用于可中断、可恢复流程（结合自动前进，停止，再执行）的编排场景
* 可用于复杂智能体系统开发（ReActAgent、TreamAgent、Multi-Agent System）


可视化设计器：

* https://solon.noear.org/flow/designer/
* 第三方开源（已组件化）：https://gitee.com/opensolon/solon-flow-bpmn-designer


嵌入第三方框架的示例：

* https://gitee.com/solonlab/solon-flow-embedded-examples
* https://gitcode.com/solonlab/solon-flow-embedded-examples
* https://github.com/solonlab/solon-flow-embedded-examples


## 主要概念


| 概念          | 简称          | 备注            | 相关接口                | 
|-------------|-------------|---------------|---------------------| 
| 流程图         | 图（或流图）      |               | Graph, GraphSpec    | 
| 流程节点        | 节点（或流节点）    | 可带任务，可带任务条件   | Node, NodeSpec      | 
| 流程连接线       | 连接（或流连接）    | 可带连接条件        | Link, LinkSpec      | 
|             |             |               |                     | 
| 流程引擎（用于执行图） | 引擎（流引擎）     |               | FlowEngine          | 
| 流程驱动器       | 驱动器（流驱动器）   |               | FlowDriver          | 
|             |             |               |                     | 
| 流程上下文       | 上下文（或流上下文）  |               | FlowContext         | 
| 流程拦截器       | 拦截器（或流拦截器）  |               | FlowInterceptor     | 



概念关系描述（就像用工具画图）：

* 一个图（Graph），由多个节点（Node）和连接（Link）组成。
* 一个节点（Node），会有多个连接（Link，也叫“流出连接”）连向别的节点。
    * 连接向其它节点，称为：流出连接。
    * 被其它节点连接，称为：流入连接。
* 一个图“必须有且只有”一个 start 类型的节点，且从 start 节点开始，顺着连接（Link）流出。
* 引擎在执行图的过程，可以有上下文（FlowContext），可以被阻断分支或停止执行（有状态流程）


通俗些，一个图就是通过 “点”（节点） + “线”（连接）画出来的一个结构。


## 五大特性展示

### 1、采用 yaml 或 json 偏平式编排格式

偏平式编排，没有深度结构（所有节点平铺，使用 link 描述连接关系）。配置简洁，关系清晰

```yaml
# c1.yml
id: "c1"
layout: 
  - { id: "n1", type: "start", link: "n2"}
  - { id: "n2", type: "activity", link: "n3"}
  - { id: "n3", type: "end"}
```

还支持简化模式（能自动推断的，都会自动处理），具体参考相关说明

```yaml
# c1.yml
id: "c1"
layout: 
  - { type: "start"}
  - { task: ""}
  - { type: "end"}
```

### 2、表达式与脚本自由

```yaml
# c2.yml
id: "c2"
layout: 
  - { type: "start"}
  - { when: "order.getAmount() >= 100", task: "order.setScore(0);"}
  - { when: "order.getAmount() > 100 && order.getAmount() <= 500", task: "order.setScore(100);"}
  - { when: "order.getAmount() > 500 && order.getAmount() <= 1000", task: "order.setScore(500);"}
  - { type: "end"}
```

### 3、元数据配置，为扩展提供了无限空间

元数据主要有两个作用：（1）为任务运行提供配置支持（2）为视图编辑提供配置支持

```yaml
# c3.yml
id: "c3"
layout: 
  - { id: "n1", type: "start", link: "n2"}
  - { id: "n2", type: "activity", link: "n3", task: "@MetaProcessCom", meta: {cc: "demo@noear.org"}}
  - { id: "n3", type: "end"}
```

通过组件方式，实现元数据的抄送配置效果

```java
@Component("MetaProcessCom")
public class MetaProcessCom implements TaskComponent {
    @Override
    public void run(FlowContext context, Node node) throws Throwable {
       String cc = node.getMeta("cc");
       if(Utils.isNotEmpty(cc)){
           //发送邮件...
       }
    }
}
```


### 4、事件广播与回调支持

广播（即只需要发送），回调（即发送后要求给答复）

```yaml
id: f4
layout:
  - task: |
      //发送事件
      context.eventBus().send("demo.topic", "hello");  //支持泛型（类型按需指定，不指定时为 object）
  - task: |
      //调用事件（就是要给答复）
      String rst = context.eventBus().<String, String>call("demo.topic.get", "hello").get();
      System.out.println(rst);
```

### 5、支持驱动定制（就像 jdbc 的驱动机制）

通过驱动定制，可方便实现：

* 工作流（workflow）， 用于办公审批型（有状态、人员参与）的编排场景
* 规则流（ruleflow）
* 数据流（dataflow）
* AI流（aiflow）
* 等...



## Solon 项目相关代码仓库


| 代码仓库                                                                        | 描述                               | 
|-----------------------------------------------------------------------------|----------------------------------| 
| [/opensolon/solon](../../../../opensolon/solon)                             | Solon ,主代码仓库                     | 
| [/opensolon/solon-examples](../../../../opensolon/solon-examples)           | Solon ,官网配套示例代码仓库                |
|                                                                             |                                  |
| [/opensolon/solon-expression](../../../../opensolon/solon-expression)       | Solon Expression ,代码仓库           | 
| [/opensolon/solon-flow](../../../../opensolon/solon-flow)                   | Solon Flow ,代码仓库                 | 
| [/opensolon/solon-ai](../../../../opensolon/solon-ai)                       | Solon Ai ,代码仓库                   | 
| [/opensolon/solon-cloud](../../../../opensolon/solon-cloud)                 | Solon Cloud ,代码仓库                | 
| [/opensolon/solon-admin](../../../../opensolon/solon-admin)                 | Solon Admin ,代码仓库                | 
| [/opensolon/solon-integration](../../../../opensolon/solon-integration)     | Solon Integration ,代码仓库          | 
| [/opensolon/solon-java17](../../../../opensolon/solon-java17)               | Solon Java17 ,代码仓库（base java17） | 
| [/opensolon/solon-java25](../../../../opensolon/solon-java25)               | Solon Java25 ,代码仓库（base java25）  | 
|                                                                             |                                  |
| [/opensolon/solon-gradle-plugin](../../../../opensolon/solon-gradle-plugin) | Solon Gradle ,插件代码仓库             | 
| [/opensolon/solon-idea-plugin](../../../../opensolon/solon-idea-plugin)     | Solon Idea ,插件代码仓库               | 
| [/opensolon/solon-vscode-plugin](../../../../opensolon/solon-vscode-plugin) | Solon VsCode ,插件代码仓库             | 
