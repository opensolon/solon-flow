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
    void testConcurrentAccessToSameInstance() throws InterruptedException {
        Graph graph = Graph.create("concurrent-access", spec -> {
            spec.addStart("start").title("开始").linkAdd("A");
            spec.addActivity("A").title("任务A").linkAdd("B");
            spec.addActivity("B").title("任务B").linkAdd("C");
            spec.addActivity("C").title("任务C").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);

        final String instanceId = "concurrent-instance";
        final int threadCount = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failCount = new AtomicInteger(0);

        // 多个线程同时访问同一个流程实例
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    FlowContext context = FlowContext.of(instanceId);

                    // 尝试获取并提交任务
                    Task task = workflowExecutor.claimTask("concurrent-access", context);
                    if (task != null && task.getState() == TaskState.WAITING) {
                        boolean submitted = workflowExecutor.submitTaskIfWaiting(
                                task, TaskAction.FORWARD, context);

                        if (submitted) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // 验证：只有一个线程应该成功提交任务
        assertEquals(1, successCount.get(), "应该有且只有一个线程成功提交任务");
        assertEquals(threadCount - 1, failCount.get(), "其他线程应该提交失败");

        // 验证流程状态
        FlowContext finalContext = FlowContext.of(instanceId);
        Node nodeA = graph.getNode("A");
        assertEquals(TaskState.COMPLETED, workflowExecutor.getState(nodeA, finalContext));

        // 下一个任务应该是B
        Task nextTask = workflowExecutor.claimTask("concurrent-access", finalContext);
        assertNotNull(nextTask);
        assertEquals("B", nextTask.getNodeId());
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
    void testComplexGraphPerformance() {
        // 创建复杂图结构进行性能测试
        Graph graph = Graph.create("complex-perf", spec -> {
            spec.addStart("start").title("开始").linkAdd("gateway1");

            // 第一层：排他网关，3个分支
            spec.addExclusive("gateway1").title("第一层选择")
                    .linkAdd("branch1-A", l -> l.when("context.model.level1 == 1"))
                    .linkAdd("branch2-A", l -> l.when("context.model.level1 == 2"))
                    .linkAdd("branch3-A", l -> l.when("context.model.level1 == 3"));

            // 每个分支有并行网关
            for (int branch = 1; branch <= 3; branch++) {
                String prefix = "branch" + branch;

                spec.addActivity(prefix + "-A").title("分支" + branch + "-A")
                        .linkAdd(prefix + "-split");

                spec.addParallel(prefix + "-split").title("分支" + branch + "并行拆分")
                        .linkAdd(prefix + "-B1")
                        .linkAdd(prefix + "-B2")
                        .linkAdd(prefix + "-B3");

                for (int parallel = 1; parallel <= 3; parallel++) {
                    spec.addActivity(prefix + "-B" + parallel)
                            .title("分支" + branch + "-并行" + parallel)
                            .linkAdd(prefix + "-merge");
                }

                spec.addParallel(prefix + "-merge").title("分支" + branch + "并行合并")
                        .linkAdd(prefix + "-C");

                spec.addActivity(prefix + "-C").title("分支" + branch + "-C")
                        .linkAdd("gateway2");
            }

            // 第二层：包容网关
            spec.addInclusive("gateway2").title("第二层包容")
                    .linkAdd("final-A", l -> l.when("context.model.optionA"))
                    .linkAdd("final-B", l -> l.when("context.model.optionB"))
                    .linkAdd("final-C", l -> l.when("context.model.optionC"));

            spec.addActivity("final-A").title("最终A").linkAdd("end");
            spec.addActivity("final-B").title("最终B").linkAdd("end");
            spec.addActivity("final-C").title("最终C").linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);

        // 测试不同路径的性能
        long totalDuration = 0;
        int testCount = 10;

        for (int i = 0; i < testCount; i++) {
            FlowContext context = FlowContext.of("perf-test-" + i);
            context.put("level1", (i % 3) + 1); // 轮流测试不同分支
            context.put("optionA", true);
            context.put("optionB", i % 2 == 0);
            context.put("optionC", i % 3 == 0);

            long startTime = System.currentTimeMillis();
            Collection<Task> tasks = workflowExecutor.findNextTasks("complex-perf", context);
            long duration = System.currentTimeMillis() - startTime;
            totalDuration += duration;

            assertFalse(tasks.isEmpty(), "应该找到至少一个任务");
        }

        long averageDuration = totalDuration / testCount;

        // 性能断言：复杂图的平均查找时间应该在50ms内
        assertTrue(averageDuration < 50,
                "平均查找耗时过长: " + averageDuration + "ms, 总耗时: " + totalDuration + "ms");
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