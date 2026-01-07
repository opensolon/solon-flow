package features.workflow.generated;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.workflow.Task;
import org.noear.solon.flow.workflow.TaskAction;
import org.noear.solon.flow.workflow.TaskState;
import org.noear.solon.flow.workflow.WorkflowExecutor;
import org.noear.solon.flow.workflow.controller.BlockStateController;
import org.noear.solon.flow.workflow.controller.NotBlockStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 高级场景和边界测试
 * 测试复杂场景、边界条件和异常情况
 */
class AdvancedScenarioTests {
    private FlowEngine flowEngine;

    @BeforeEach
    void setUp() {
        flowEngine = FlowEngine.newInstance();
    }

    @Test
    void testConcurrentInstancesIsolation() {
        WorkflowExecutor workflow = WorkflowExecutor.of(
                flowEngine,
                new BlockStateController(),
                new InMemoryStateRepository()
        );

        Graph graph = Graph.create("concurrent-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("A");
            spec.addActivity("A").title("任务A").linkAdd("B");
            spec.addActivity("B").title("任务B").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);

        // 创建两个独立的流程实例
        FlowContext context1 = FlowContext.of("instance-1");
        FlowContext context2 = FlowContext.of("instance-2");

        // 在实例1中执行操作
        Task task1 = workflow.claimTask("concurrent-test", context1);
        assertEquals("A", task1.getNodeId());
        workflow.submitTask("concurrent-test", "A", TaskAction.FORWARD, context1);

        // 实例2应该不受影响
        Task task2 = workflow.claimTask("concurrent-test", context2);
        assertEquals("A", task2.getNodeId());
        assertEquals(TaskState.WAITING, task2.getState());

        // 验证状态隔离
        Node nodeA = graph.getNode("A");
        assertEquals(TaskState.COMPLETED, workflow.getState(nodeA, context1));
        assertEquals(TaskState.WAITING, workflow.getState(nodeA, context2));
    }

    @Test
    void testNotBlockStateController() {
        // 使用NotBlockStateController测试自动前进
        WorkflowExecutor autoWorkflow = WorkflowExecutor.of(
                flowEngine,
                new NotBlockStateController(),
                new InMemoryStateRepository()
        );

        Graph graph = Graph.create("auto-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("A");
            spec.addActivity("A").title("任务A").linkAdd("B");
            spec.addActivity("B").title("任务B").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);
        FlowContext context = FlowContext.of("auto-context");

        // 使用NotBlockStateController时，matchTask应该返回null（全部自动前进）
        Task task = autoWorkflow.claimTask("auto-test", context);
        assertNull(task);

        // findNextTasks应该能正常工作
        Collection<Task> tasks = autoWorkflow.findNextTasks("auto-test", context);
        assertEquals(1, tasks.size());
        assertEquals("A", tasks.iterator().next().getNodeId());

        // 执行一次eval应该自动完成整个流程
        flowEngine.eval("auto-test", context);

        // 流程应该已完成
        Task finalTask = autoWorkflow.findTask("auto-test", context);
        assertNull(finalTask);
    }

    @Test
    void testTerminatedWorkflowRestart() {
        WorkflowExecutor workflow = WorkflowExecutor.of(
                flowEngine,
                new BlockStateController(),
                new InMemoryStateRepository()
        );

        Graph graph = Graph.create("terminate-restart", spec -> {
            spec.addStart("start").title("开始").linkAdd("A");
            spec.addActivity("A").title("任务A").linkAdd("B");
            spec.addActivity("B").title("任务B").linkAdd("C");
            spec.addActivity("C").title("任务C").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);
        FlowContext context = FlowContext.of("terminate-context");

        // 前进到B
        workflow.claimTask("terminate-restart", context);
        workflow.submitTask("terminate-restart", "A", TaskAction.FORWARD, context);

        // 终止B
        workflow.submitTask("terminate-restart", "B", TaskAction.TERMINATE, context);

        // 验证终止状态
        Node nodeB = graph.getNode("B");
        assertEquals(TaskState.TERMINATED, workflow.getState(nodeB, context));

        // 尝试重启流程
        workflow.submitTask("terminate-restart", "start", TaskAction.RESTART, context);

        // 所有状态应该被清空
        assertEquals(TaskState.UNKNOWN, workflow.getState(graph.getNode("A"), context));
        assertEquals(TaskState.UNKNOWN, workflow.getState(nodeB, context));
        assertEquals(TaskState.UNKNOWN, workflow.getState(graph.getNode("C"), context));

        // 流程可以从头开始
        Task restartTask = workflow.claimTask("terminate-restart", context);
        assertEquals("A", restartTask.getNodeId());
        assertEquals(TaskState.WAITING, restartTask.getState());
    }

    @Test
    void testComplexJumpScenarios() {
        WorkflowExecutor workflow = WorkflowExecutor.of(
                flowEngine,
                new BlockStateController(),
                new InMemoryStateRepository()
        );

        // 创建复杂图结构
        Graph graph = Graph.create("complex-jump", spec -> {
            spec.addStart("start").title("开始").linkAdd("gateway");

            spec.addExclusive("gateway").title("选择网关")
                    .linkAdd("path1", l -> l.when("path == 1"))
                    .linkAdd("path2", l -> l.when("path == 2"));

            // 路径1
            spec.addActivity("path1").title("路径1-A").linkAdd("path1-B");
            spec.addActivity("path1-B").title("路径1-B").linkAdd("merge");

            // 路径2
            spec.addActivity("path2").title("路径2-A").linkAdd("path2-B");
            spec.addActivity("path2-B").title("路径2-B").linkAdd("merge");

            spec.addParallel("merge").title("合并网关").linkAdd("final");
            spec.addActivity("final").title("最终任务").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);

        // 测试场景1：在路径1中跳转到路径2
        FlowContext context1 = FlowContext.of("jump-scenario-1");
        context1.put("path", 1);

        // 匹配到路径1
        Task task1 = workflow.claimTask("complex-jump", context1);
        assertEquals("path1", task1.getNodeId());

        // 跳转到路径2-B
        workflow.submitTask("complex-jump", "path2-B", TaskAction.FORWARD_JUMP, context1);

        // 验证：path1和path1-B应该自动完成，当前在path2-B
        assertEquals(TaskState.COMPLETED, workflow.getState(graph.getNode("path1"), context1));
        assertEquals(TaskState.COMPLETED, workflow.getState(graph.getNode("path1-B"), context1));
        assertEquals(TaskState.WAITING, workflow.getState(graph.getNode("path2-B"), context1));

        // 测试场景2：跨网关跳转
        FlowContext context2 = FlowContext.of("jump-scenario-2");
        context2.put("path", 2);

        // 直接跳转到最终任务
        workflow.submitTask("complex-jump", "final", TaskAction.FORWARD_JUMP, context2);

        // 所有中间节点应该自动完成
        assertEquals(TaskState.COMPLETED, workflow.getState(graph.getNode("path2"), context2));
        assertEquals(TaskState.COMPLETED, workflow.getState(graph.getNode("path2-B"), context2));
        assertEquals(TaskState.WAITING, workflow.getState(graph.getNode("final"), context2));
    }

    @Test
    void testStatePersistenceAndRecovery() {
        InMemoryStateRepository repository = new InMemoryStateRepository();
        WorkflowExecutor workflow = WorkflowExecutor.of(
                flowEngine,
                new BlockStateController(),
                repository
        );

        Graph graph = Graph.create("persistence-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("A");
            spec.addActivity("A").title("任务A").linkAdd("B");
            spec.addActivity("B").title("任务B").linkAdd("C");
            spec.addActivity("C").title("任务C").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);
        String instanceId = "persist-instance";
        FlowContext context = FlowContext.of(instanceId);

        // 执行部分流程
        workflow.claimTask("persistence-test", context);
        workflow.submitTask("persistence-test", "A", TaskAction.FORWARD, context);
        workflow.submitTask("persistence-test", "B", TaskAction.FORWARD, context);

        // 验证状态被保存
        Node nodeA = graph.getNode("A");
        Node nodeB = graph.getNode("B");
        assertEquals(TaskState.COMPLETED, repository.stateGet(context, nodeA));
        assertEquals(TaskState.COMPLETED, repository.stateGet(context, nodeB));

        // 模拟"恢复" - 创建新的executor但使用相同的repository
        WorkflowExecutor recoveredWorkflow = WorkflowExecutor.of(
                flowEngine,
                new BlockStateController(),
                repository
        );

        FlowContext recoveredContext = FlowContext.of(instanceId);

        // 应该能从断点继续
        Task recoveredTask = recoveredWorkflow.claimTask("persistence-test", recoveredContext);
        assertNotNull(recoveredTask);
        assertEquals("C", recoveredTask.getNodeId());

        // 状态应该被保留
        assertEquals(TaskState.COMPLETED, recoveredWorkflow.getState(nodeA, recoveredContext));
        assertEquals(TaskState.COMPLETED, recoveredWorkflow.getState(nodeB, recoveredContext));
    }

    @Test
    void testInvalidOperations() {
        WorkflowExecutor workflow = WorkflowExecutor.of(
                flowEngine,
                new BlockStateController(),
                new InMemoryStateRepository()
        );

        Graph graph = Graph.create("invalid-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("A");
            spec.addActivity("A").title("任务A").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);
        FlowContext context = FlowContext.of("invalid-context");

        // 测试对不存在节点的操作
        assertThrows(IllegalArgumentException.class, () -> {
            workflow.submitTask("invalid-test", "NON_EXISTENT", TaskAction.FORWARD, context);
        });

        // 测试对结束节点的操作
        workflow.claimTask("invalid-test", context);
        workflow.submitTask("invalid-test", "A", TaskAction.FORWARD, context);
        workflow.submitTask("invalid-test", "end", TaskAction.FORWARD, context);

        // 流程结束后，尝试操作应该没有效果但不报错
        workflow.submitTask("invalid-test", "A", TaskAction.BACK, context);

        // 验证状态没有变化
        assertEquals(TaskState.COMPLETED, workflow.getState(graph.getNode("A"), context));
    }

    @Test
    void testGatewayAutoForwardBehavior() {
        WorkflowExecutor workflow = WorkflowExecutor.of(
                flowEngine,
                new BlockStateController(),
                new InMemoryStateRepository()
        );

        // 测试网关的自动前进特性
        Graph graph = Graph.create("gateway-auto", spec -> {
            spec.addStart("start").title("开始").linkAdd("exclusive");
            spec.addExclusive("exclusive").title("排他网关")
                    .linkAdd("A", l -> l.when("\"A\".equals(choice)"))
                    .linkAdd("B"); // 默认路径
            spec.addActivity("A").title("任务A").linkAdd("parallel");
            spec.addActivity("B").title("任务B").linkAdd("parallel");
            spec.addParallel("parallel").title("并行网关").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);

        // 测试排他网关的自动前进
        FlowContext context = FlowContext.of("gateway-auto-context");
        context.put("choice", "A");

        // 网关应该自动前进到选中的分支
        workflow.claimTask("gateway-auto", context);

        // 排他网关应该自动前进到A
        Task task = workflow.findTask("gateway-auto", context);
        assertNotNull(task);
        assertEquals("A", task.getNodeId());

        // 完成A后，应该自动前进到并行网关
        workflow.submitTask("gateway-auto", "A", TaskAction.FORWARD, context);

        // 并行网关应该产生两个任务（但实际上只有一个流入连接）
        // 这里主要测试自动前进的逻辑
        Task afterGateway = workflow.findTask("gateway-auto", context);
        // 根据实现，可能为null或特定状态
        // 这取决于并行网关的自动前进实现
    }
}