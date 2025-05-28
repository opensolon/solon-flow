<h1 align="center" style="text-align:center;">
<img src="solon_icon.png" width="128" />
<br />
Solon-Flow
</h1>
<p align="center">
	<strong>Java Flow 通用流编排应用开发框架（支持已知流编排的各种场景）</strong>
    <br/>
    <strong>【基于 Solon 应用开发框架构建】</strong>
</p>
<p align="center">
	<a href="https://solon.noear.org/article/learn-solon-flow">https://solon.noear.org/article/learn-solon-flow</a>
</p>

<p align="center">
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
    <a target="_blank" href="https://www.oracle.com/java/technologies/javase/jdk23-archive-downloads.html">
		<img src="https://img.shields.io/badge/JDK-23-green.svg" alt="jdk-23" />
	</a>
    <br />
    <a target="_blank" href='https://gitee.com/noear/solon/stargazers'>
		<img src='https://gitee.com/noear/solon/badge/star.svg' alt='gitee star'/>
	</a>
    <a target="_blank" href='https://github.com/noear/solon/stargazers'>
		<img src="https://img.shields.io/github/stars/noear/solon.svg?style=flat&logo=github" alt="github star"/>
	</a>
    <a target="_blank" href='https://gitcode.com/opensolon/solon/star'>
		<img src='https://gitcode.com/opensolon/solon/star/badge.svg' alt='gitcode star'/>
	</a>
</p>

<hr />


## 简介

面向全场景的 Java Flow （流编排）应用开发框架。是 Solon 项目的一部分。也可嵌入到 SpringBoot2、jFinal、Vert.x 等框架中使用。

支持已知流编排的各种场景:

* 可用于计算（或任务）的编排场景
* 可用于业务规则和决策处理型的编排场景
* 可用于办公审批型（有状态、可中断，人员参与）的编排场景
* 可用于长时间流程（结合自动前进，等待介入）的编排场景


可视化设计器：

* https://solon.noear.org/flow/designer/


嵌入第三方框架的示例：

* https://gitee.com/opensolon/solon-flow-embedded-examples
* https://gitcode.com/opensolon/solon-flow-embedded-examples
* https://github.com/opensolon/solon-flow-embedded-examples


## 六大特性展示


### 1、使用 yaml 格式

配置简洁，关系清晰。内容多了后会像 docker-compose。

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

### 3、元数据配置，为扩展提供了无限空间（每个流程，相当于自带了元数据库）

```yaml
# c3.yml
id: "c3"
layout: 
  - { id: "n1", type: "start", link: "n2"}
  - { id: "n2", type: "activity", link: "n3", meta: {cc: "demo@noear.org"}, task: "@MetaProcessCom"}
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

也可通过驱动定制方式，实现抄送效果（显得重一些）

```java
public class OaFlowDriver extends SimpleFlowDriver {
    @Override
    public void handleTask(FlowContext context, Task task) throws Throwable {
        if (Utils.isEmpty(task.getDescription())) {
           String cc = task.getNode().getMeta("cc");
           if(Utils.isNotEmpty(cc)){
               //发送邮件
           }
        } else {
            super.handleTask(context, task);
        }
    }
}

//FlowEngine flowEngine = FlowEngine.newInstance();
//flowEngine.register(new OaFlowDriver()); //替换掉默认驱动
```

### 4、事件广播与回调支持

广播（即只需要发送），回调（即发送后要求给答复）

```yaml
id: f4
layout:
  - task: |
      //只发送
      context.<String,String>eventBus().send("demo.topic", "hello");  //支持泛型（类型按需指定，不指定时为 object）
  - task: |
      //发送并要求响应（就是要给答复）
      String rst = context.<String,String>eventBus().sendAndRequest("demo.topic.get", "hello");
      System.out.println(rst);
```

### 5、支持“无状态”、“有状态”两种需求分类

支持丰富的应用场景：

* 可用于计算（或任务）的编排场景
* 可用于业务规则和决策处理型的编排场景
* 可用于办公审批型（有状态、可中断，人员参与）的编排场景
* 可用于长时间流程（结合自动前进，等待介入）的编排场景

自身也相当于一个低代码的运行引擎（单个 yml 文件，也可满足所有的执行需求）。


### 6、驱动定制（是像 JDBC 有 MySql, PostgreSQL，还可能有 Elasticsearch）

这是一个定制后的，支持基于状态驱动的流引擎效果。
```java
StatefulFlowEngine flowEngine = new StatefulFlowEngine(StatefulSimpleFlowDriver.builder()
                .stateOperator(new MetaStateOperator("actor"))
                .stateRepository(new InMemoryStateRepository())
                .build());
                
var context = new StatefulFlowContext("i1").put("actor", "陈鑫");

//获取上下文用户的活动节点
var statefulNode = flowEngine.getActivityNode("f1", context);

assert "step2".equals(statefulNode.getNode().getId());
assert StateType.UNKNOWN == statefulNode.getState(); //没有权限启动任务（因为没有配置操作员）

//提交活动状态
flowEngine.postActivityState(context, "f1", statefulNode.getNode().getId(), StateType.COMPLETED);
```

流程配置样例：

```yaml
id: f1
layout:
  - {id: step1, title: "发起审批", type: "start"}
  - {id: step2, title: "抄送", meta: {cc: "吕方"}, task: "@OaMetaProcessCom"}
  - {id: step3, title: "审批", meta: {actor: "陈鑫", cc: "吕方"}, task: "@OaMetaProcessCom"}
  - {id: step4, title: "审批", type: "parallel", link: [step4_1, step4_2]}
  - {id: step4_1, meta: {actor: "陈宇"}, link: step4_end}
  - {id: step4_2, meta: {actor: "吕方"}, link: step4_end}
  - {id: step4_end, type: "parallel"}
  - {id: step5, title: "抄送", meta: {cc: "吕方"}, task: "@OaMetaProcessCom"}
  - {id: step6, title: "结束", type: "end"}
```

对于驱动器的定制，我们还可以：定制（或选择）不同的脚本执行器、组件容器实现等。




## Solon 项目相关代码仓库


| 代码仓库                                                             | 描述                               | 
|------------------------------------------------------------------|----------------------------------| 
| [/opensolon/solon](../../../../opensolon/solon)                             | Solon ,主代码仓库                     | 
| [/opensolon/solon-examples](../../../../opensolon/solon-examples)           | Solon ,官网配套示例代码仓库                |
|                                                                  |                                  |
| [/opensolon/solon-expression](../../../../opensolon/solon-expression)                   | Solon Expression ,代码仓库           | 
| [/opensolon/solon-flow](../../../../opensolon/solon-flow)                   | Solon Flow ,代码仓库                 | 
| [/opensolon/solon-ai](../../../../opensolon/solon-ai)                       | Solon Ai ,代码仓库                   | 
| [/opensolon/solon-cloud](../../../../opensolon/solon-cloud)                 | Solon Cloud ,代码仓库                | 
| [/opensolon/solon-admin](../../../../opensolon/solon-admin)                 | Solon Admin ,代码仓库                | 
| [/opensolon/solon-jakarta](../../../../opensolon/solon-jakarta)             | Solon Jakarta ,代码仓库（base java21） | 
| [/opensolon/solon-integration](../../../../opensolon/solon-integration)     | Solon Integration ,代码仓库          | 
|                                                                  |                                  |
| [/opensolon/solon-gradle-plugin](../../../../opensolon/solon-gradle-plugin) | Solon Gradle ,插件代码仓库             | 
| [/opensolon/solon-idea-plugin](../../../../opensolon/solon-idea-plugin)     | Solon Idea ,插件代码仓库               | 
| [/opensolon/solon-vscode-plugin](../../../../opensolon/solon-vscode-plugin) | Solon VsCode ,插件代码仓库             | 

