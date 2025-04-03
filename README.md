<h1 align="center" style="text-align:center;">
<img src="solon_icon.png" width="128" />
<br />
Solon
</h1>
<p align="center">
	<strong>面向全场景的 Java 企业级应用开发框架：克制、高效、开放、生态</strong>
    <br/>
    <strong>【开放原子开源基金会，孵化项目】</strong>
</p>
<p align="center">
	<a href="https://solon.noear.org/">https://solon.noear.org</a>
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

<p align="center">
并发高 300%；内存省 50%；启动快 10 倍；打包小 90%；同时支持 java8 ~ java23, native 运行时。
<br/>
从零开始构建，有更灵活的接口规范与开放生态
</p>

<hr />

## 主要代码仓库


| 代码仓库                                            | 描述                               | 
|-------------------------------------------------|----------------------------------| 
| https://gitee.com/opensolon/solon               | Solon ,主代码仓库                     | 
| https://gitee.com/opensolon/solon-examples      | Solon ,官网配套示例代码仓库                |
|                                                 |                                  |
| https://gitee.com/opensolon/solon-ai            | Solon Ai ,代码仓库                   | 
| https://gitee.com/opensolon/solon-flow          | Solon Flow ,代码仓库                 | 
| https://gitee.com/opensolon/solon-cloud         | Solon Cloud ,代码仓库                | 
| https://gitee.com/opensolon/solon-admin         | Solon Admin ,代码仓库                | 
| https://gitee.com/opensolon/solon-jakarta       | Solon Jakarta ,代码仓库（base java17） | 
| https://gitee.com/opensolon/solon-integration   | Solon Integration ,代码仓库          | 
|                                                 |                                  |
| https://gitee.com/opensolon/solon-gradle-plugin | Solon Gradle ,插件代码仓库             | 
| https://gitee.com/opensolon/solon-idea-plugin   | Solon Idea ,插件代码仓库               | 
| https://gitee.com/opensolon/solon-vscode-plugin | Solon VsCode ,插件代码仓库             | 
|                                                 |                                  |
| https://gitee.com/dromara/solon-plugins         | Solon 第三方扩展插件代码仓库                | 

## 应用示例

solon-flow 是一个通用的流处理引擎，支持：计算编排、业务规则处理、行政审批支持，等多场景支持。

### 1、计算编排（Hello world）

```yaml
# classpath:flow/c1.chain.yml
id: "c1"
layout: 
  - { id: "n1", type: "start", link: "n2"}
  - { id: "n2", type: "execute", link: "n3", task: "System.out.println(\"hello world!\");"}
  - { id: "n3", type: "end"}
```

```java
@Component
public class DemoCom implements LifecycleBean {
    @Inject 
    private FlowEngine flowEngine;
    
    @Override
    public void start() throws Throwable {
        flowEngine.eval("c1");
    }
}
```

### 2、业务评分示例

```yaml
# r1.chain.yml
id: "r1"
title: "评分规则"
layout:
  - { type: "start"}
  - { when: "order.getAmount() >= 100", task: "order.setScore(0);"}
  - { when: "order.getAmount() > 100 && order.getAmount() <= 500", task: "order.setScore(100);"}
  - { when: "order.getAmount() > 500 && order.getAmount() <= 1000", task: "order.setScore(500);"}
  - { type: "end"}
```

```java
@Component
public class DemoCom implements LifecycleBean {
    @Inject 
    private FlowEngine flowEngine;
    
    @Override
    public void start() throws Throwable {
        FlowContext context = new FlowContext();
        context.put("order", new OrderModel());
        
        flowEngine.eval("r1", context);
    }
}
```

### 3、行政审批示例（支持状态持久化）

```yaml
# e1.chain.yml
id: e1
layout:
  - {id: step1, title: "发起审批", meta: {actor: "刘涛", form: "form1"}}
  - {id: step2, title: "抄送", meta: {cc: "吕方"}, task: "@OaMetaProcessCom"}
  - {id: step3, title: "审批", meta: {actor: "陈鑫", cc: "吕方"}, task: "@OaMetaProcessCom"}
  - {id: step4, title: "审批", type: "parallel", link: [step4_1, step4_2]}
  - {id: step4_1, meta: {actor: "陈宇"}, link: step4_end}
  - {id: step4_2, meta: {actor: "吕方"}, link: step4_end}
  - {id: step4_end, type: "parallel"}
  - {id: step5, title: "抄送", meta: {cc: "吕方"}, task: "@OaMetaProcessCom"}
  - {id: step6, title: "结束", type: "end"}
```

```java
@Configuration
public class DemoConfig {
    @Bean
    public StatefulFlowEngine statefulFlowEngine() {
        StatefulFlowEngine flowEngine = new StatefulFlowEngine(StatefulSimpleFlowDriver.builder()
                .stateOperator(new MetaStateOperator())
                .stateRepository(new InMemoryStateRepository()) //状态仓库（支持持久化）
                .build());

        flowEngine.load("classpath:flow/*.yml");
        
        return flowEngine;
    }

    @Bean
    public void test(StatefulFlowEngine flowEngine) {
        String instanceId = Utils.uuid();
        String chainId = "e1";

        FlowContext context = getContext(instanceId,"刘涛");
        StatefulNode statefulNode = flowEngine.getActivityNode(chainId, context);
        assert "step1".equals(statefulNode.getNode().getId());
        assert NodeState.WAITING == statefulNode.getState(); //等待当前用户处理

        //提交状态
        context.put("op", "通过"); //用于扩展状态记录
        flowEngine.postActivityState(context, statefulNode.getNode(), NodeState.COMPLETED);
    }

    private FlowContext getContext(String instanceId, String actor) {
        FlowContext context = new FlowContext(instanceId);
        context.put("actor", actor);
        return context;
    }
}
```