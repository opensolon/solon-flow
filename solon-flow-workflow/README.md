

# solon-flow-workflow

轻量级审批型工作流引擎


示例:

```java
public class Demo {
    public void demo(FlowEngine engine, Graph graph) {
        //0. 初始化工作流
        WorkflowService workflow = WorkflowService.of(engine, WorkflowDriver.builder()
                .stateController(new ActorStateController())
                .stateRepository(new InMemoryStateRepository())
                .build());

        FlowContext context = FlowContext.of("case1");

        //1. 取出任务
        Task task = workflow.getTask(graph, context);

        //2. 提交任务
        workflow.postTask(task.getNode(), TaskAction.FORWARD, context);
    }
}
```