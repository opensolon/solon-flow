package features.workflow.generated;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.noear.solon.flow.*;
import org.noear.solon.flow.workflow.Task;
import org.noear.solon.flow.workflow.TaskAction;
import org.noear.solon.flow.workflow.TaskState;
import org.noear.solon.flow.workflow.WorkflowExecutor;
import org.noear.solon.flow.workflow.controller.BlockStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试和性能测试
 * 测试并发性能、大规模流程等场景
 */
class IntegrationAndPerformanceTests {
    private FlowEngine flowEngine;
    private WorkflowExecutor workflowExecutor;

    @BeforeEach
    void setUp() {
        flowEngine = FlowEngine.newInstance();
        workflowExecutor = WorkflowExecutor.of(
                flowEngine,
                new BlockStateController(),
                new InMemoryStateRepository()
        );
    }

    @Test
    @Timeout(5)
    void testLargeScaleLinearGraph() {
        // 创建大规模线性图（100个节点）
        Graph graph = Graph.create("large-linear", spec -> {
            spec.addStart("start").title("开始").linkAdd("node-1");

            for (int i = 1; i <= 100; i++) {
                String nodeId = "node-" + i;
                String nextId = (i < 100) ? "node-" + (i + 1) : "end";

                spec.addActivity(nodeId).title("任务" + i).linkAdd(nextId);
            }

            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);
        FlowContext context = FlowContext.of("large-context");

        // 测试查找性能
        long startTime = System.currentTimeMillis();
        Collection<Task> tasks = workflowExecutor.findNextTasks("large-linear", context);
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(1, tasks.size());
        assertEquals("node-1", tasks.iterator().next().getNodeId());

        // 性能断言：100个节点的查找应该在100ms内完成
        assertTrue(duration < 100, "查找耗时过长: " + duration + "ms");

        // 测试完整执行性能
        startTime = System.currentTimeMillis();
        for (int i = 1; i <= 100; i++) {
            workflowExecutor.submitTask("large-linear", "node-" + i, TaskAction.FORWARD, context);
        }
        duration = System.currentTimeMillis() - startTime;

        // 性能断言：100个节点的执行应该在500ms内完成
        assertTrue(duration < 500, "执行耗时过长: " + duration + "ms");
    }



    @Test
    @Timeout(10)
    void testMultipleIndependentInstances() throws InterruptedException {
        Graph graph = Graph.create("multi-instance", spec -> {
            spec.addStart("start").title("开始").linkAdd("A");
            spec.addActivity("A").title("任务A").linkAdd("B");
            spec.addActivity("B").title("任务B").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);

        final int instanceCount = 50;
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final CountDownLatch latch = new CountDownLatch(instanceCount);
        final AtomicInteger completedCount = new AtomicInteger(0);

        // 创建并执行多个独立的流程实例
        for (int i = 0; i < instanceCount; i++) {
            final int instanceNum = i;
            executor.submit(() -> {
                try {
                    String instanceId = "instance-" + instanceNum;
                    FlowContext context = FlowContext.of(instanceId);

                    // 执行完整流程
                    workflowExecutor.claimTask("multi-instance", context);
                    workflowExecutor.submitTask("multi-instance", "A", TaskAction.FORWARD, context);
                    workflowExecutor.submitTask("multi-instance", "B", TaskAction.FORWARD, context);

                    // 验证完成
                    Task finalTask = workflowExecutor.claimTask("multi-instance", context);
                    assertNull(finalTask, "实例 " + instanceId + " 应该已完成");

                    completedCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // 所有实例都应该成功完成
        assertEquals(instanceCount, completedCount.get(), "所有实例都应该成功完成");
    }


    @Test
    void testMemoryUsageWithManyStates() {
        InMemoryStateRepository repository = new InMemoryStateRepository();
        WorkflowExecutor workflow = WorkflowExecutor.of(
                flowEngine,
                new BlockStateController(),
                repository
        );

        Graph graph = Graph.create("memory-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("A");
            spec.addActivity("A").title("任务A").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);

        // 创建大量流程实例
        int instanceCount = 1000;
        for (int i = 0; i < instanceCount; i++) {
            String instanceId = "memory-instance-" + i;
            FlowContext context = FlowContext.of(instanceId);

            // 部分实例完成，部分进行中
            if (i % 2 == 0) {
                workflow.claimTask("memory-test", context);
                workflow.submitTask("memory-test", "A", TaskAction.FORWARD, context);
            } else {
                workflow.claimTask("memory-test", context);
                // 不提交，保持等待状态
            }
        }

        // 验证所有实例的状态都被正确管理
        for (int i = 0; i < instanceCount; i++) {
            String instanceId = "memory-instance-" + i;
            FlowContext context = FlowContext.of(instanceId);
            Node nodeA = graph.getNode("A");

            TaskState state = workflow.getState(nodeA, context);
            if (i % 2 == 0) {
                assertEquals(TaskState.COMPLETED, state,
                        "实例 " + instanceId + " 应该已完成");
            } else {
                assertEquals(TaskState.WAITING, state,
                        "实例 " + instanceId + " 应该在等待中");
            }
        }

        // 清理部分实例的状态
        for (int i = 0; i < instanceCount; i += 3) {
            String instanceId = "memory-instance-" + i;
            FlowContext context = FlowContext.of(instanceId);
            repository.stateClear(context);
        }

        // 验证清理后的状态
        for (int i = 0; i < instanceCount; i++) {
            String instanceId = "memory-instance-" + i;
            FlowContext context = FlowContext.of(instanceId);
            Node nodeA = graph.getNode("A");

            TaskState state = workflow.getState(nodeA, context);
            if (i % 3 == 0) {
                assertEquals(TaskState.UNKNOWN, state,
                        "实例 " + instanceId + " 应该已被清理");
            }
        }
    }
}