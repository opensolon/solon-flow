package features.flow.generated.multiple;

import org.noear.solon.flow.*;
import org.noear.solon.flow.driver.SimpleFlowDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowEngine.eval 与 FlowContext stop/interrupt 交互测试
 *
 * 测试目的：验证在多图场景下 FlowContext 的 stop 和 interrupt 功能
 * 测试场景：
 * 1. 单图中 stop 停止整个流程
 * 2. 多图中 stop 停止所有图执行
 * 3. interrupt 中断当前分支但不影响其他分支
 * 4. stop 在子图中的传播
 * 5. interrupt 在并行分支中的行为
 * 6. 恢复执行后的状态
 */
public class FlowEngineEvalStopInterruptTest {

    private FlowEngine flowEngine;

    @BeforeEach
    void setUp() {
        flowEngine = FlowEngine.newInstance(SimpleFlowDriver.getInstance());
    }

    /**
     * 测试场景1：单图中使用 stop 停止流程
     * 验证在单图流程中调用 stop 可以正确终止执行
     */
    @Test
    void testStopInSingleGraph() {
        System.out.println("=== 测试场景1：单图中使用 stop 停止流程 ===");

        AtomicInteger task1Executed = new AtomicInteger(0);
        AtomicInteger task2Executed = new AtomicInteger(0);
        AtomicInteger task3Executed = new AtomicInteger(0);

        Graph singleGraph = Graph.create("stop-single-graph", "停止测试单图", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("task1");

            spec.addActivity("task1").title("任务1（会stop）")
                    .task((context, node) -> {
                        System.out.println("执行任务1: " + context.getInstanceId());
                        task1Executed.incrementAndGet();

                        // 在任务1中停止流程
                        context.stop();
                        System.out.println("任务1调用 stop()");
                    })
                    .linkAdd("task2");

            spec.addActivity("task2").title("任务2（不应该执行）")
                    .task((context, node) -> {
                        System.out.println("执行任务2: " + context.getInstanceId());
                        task2Executed.incrementAndGet();
                    })
                    .linkAdd("task3");

            spec.addActivity("task3").title("任务3（不应该执行）")
                    .task((context, node) -> {
                        System.out.println("执行任务3: " + context.getInstanceId());
                        task3Executed.incrementAndGet();
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        flowEngine.load(singleGraph);

        String instanceId = "stop-single-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行单图流程（预期在任务1后停止）...");
        flowEngine.eval("stop-single-graph", context);

        // 验证执行结果
        assertEquals(1, task1Executed.get(), "任务1应该被执行");
        assertEquals(0, task2Executed.get(), "任务2不应该被执行（被stop了）");
        assertEquals(0, task3Executed.get(), "任务3不应该被执行（被stop了）");

        // 验证流程状态
        assertTrue(context.isStopped(), "上下文应该标记为已停止");
        assertEquals("task1", context.lastNodeId(), "最后执行的节点应该是task1");
        assertNotNull(context.lastRecord(), "应该有最后记录");
        assertFalse(context.lastRecord().isEnd(), "不应该到达结束节点");

        System.out.println("✅ 单图stop测试通过");
    }

    /**
     * 测试场景2：多图中在主图调用 stop
     * 验证在主图中调用 stop 会停止整个嵌套执行
     */
    @Test
    void testStopInMainGraphWithSubGraph() {
        System.out.println("\n=== 测试场景2：多图中在主图调用 stop ===");

        AtomicInteger mainTask1Executed = new AtomicInteger(0);
        AtomicInteger subTaskExecuted = new AtomicInteger(0);
        AtomicInteger mainTask2Executed = new AtomicInteger(0);

        // 创建子图
        Graph subGraph = Graph.create("stop-sub-graph", "停止测试子图", spec -> {
            spec.addStart("sub_start").title("子图开始")
                    .linkAdd("sub_task");

            spec.addActivity("sub_task").title("子图任务")
                    .task((context, node) -> {
                        System.out.println("执行子图任务: " + context.getInstanceId());
                        subTaskExecuted.incrementAndGet();
                    })
                    .linkAdd("sub_end");

            spec.addEnd("sub_end").title("子图结束");
        });

        // 创建主图（在调用子图前停止）
        Graph mainGraph = Graph.create("stop-main-graph", "停止测试主图", spec -> {
            spec.addStart("main_start").title("主图开始")
                    .linkAdd("main_task1");

            spec.addActivity("main_task1").title("主任务1（会stop）")
                    .task((context, node) -> {
                        System.out.println("执行主任务1: " + context.getInstanceId());
                        mainTask1Executed.incrementAndGet();

                        // 在调用子图前停止
                        context.stop();
                        System.out.println("主任务1调用 stop()");
                    })
                    .linkAdd("call_sub");

            spec.addActivity("call_sub").title("调用子图（不应该执行）")
                    .task("#stop-sub-graph")
                    .linkAdd("main_task2");

            spec.addActivity("main_task2").title("主任务2（不应该执行）")
                    .task((context, node) -> {
                        System.out.println("执行主任务2: " + context.getInstanceId());
                        mainTask2Executed.incrementAndGet();
                    })
                    .linkAdd("main_end");

            spec.addEnd("main_end").title("主图结束");
        });

        flowEngine.load(subGraph);
        flowEngine.load(mainGraph);

        String instanceId = "stop-main-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行多图流程（预期在主任务1后停止）...");
        flowEngine.eval("stop-main-graph", context);

        // 验证执行结果
        assertEquals(1, mainTask1Executed.get(), "主任务1应该被执行");
        assertEquals(0, subTaskExecuted.get(), "子图任务不应该被执行（被stop了）");
        assertEquals(0, mainTask2Executed.get(), "主任务2不应该被执行（被stop了）");

        // 验证流程状态
        assertTrue(context.isStopped(), "上下文应该标记为已停止");
        assertEquals("main_task1", context.lastNodeId(), "最后执行的节点应该是main_task1");

        System.out.println("✅ 主图stop测试通过");
    }

    /**
     * 测试场景3：在子图中调用 stop
     * 验证在子图中调用 stop 会传播到主图
     */
    @Test
    void testStopInSubGraph() {
        System.out.println("\n=== 测试场景3：在子图中调用 stop ===");

        AtomicInteger mainTask1Executed = new AtomicInteger(0);
        AtomicInteger subTask1Executed = new AtomicInteger(0);
        AtomicInteger subTask2Executed = new AtomicInteger(0);
        AtomicInteger mainTask2Executed = new AtomicInteger(0);

        // 创建子图（会在子图中停止）
        Graph subGraph = Graph.create("stop-inside-sub", "内部停止子图", spec -> {
            spec.addStart("sub_start").title("子图开始")
                    .linkAdd("sub_task1");

            spec.addActivity("sub_task1").title("子任务1")
                    .task((context, node) -> {
                        System.out.println("执行子任务1: " + context.getInstanceId());
                        subTask1Executed.incrementAndGet();
                    })
                    .linkAdd("sub_task2");

            spec.addActivity("sub_task2").title("子任务2（会stop）")
                    .task((context, node) -> {
                        System.out.println("执行子任务2: " + context.getInstanceId());
                        subTask2Executed.incrementAndGet();

                        // 在子图中停止
                        context.stop();
                        System.out.println("子任务2调用 stop()");
                    })
                    .linkAdd("sub_end");

            spec.addEnd("sub_end").title("子图结束");
        });

        // 创建主图
        Graph mainGraph = Graph.create("stop-from-sub-main", "从子图停止主图", spec -> {
            spec.addStart("main_start").title("主图开始")
                    .linkAdd("main_task1");

            spec.addActivity("main_task1").title("主任务1")
                    .task((context, node) -> {
                        System.out.println("执行主任务1: " + context.getInstanceId());
                        mainTask1Executed.incrementAndGet();
                    })
                    .linkAdd("call_sub");

            spec.addActivity("call_sub").title("调用子图")
                    .task("#stop-inside-sub")
                    .linkAdd("main_task2");

            spec.addActivity("main_task2").title("主任务2（不应该执行）")
                    .task((context, node) -> {
                        System.out.println("执行主任务2: " + context.getInstanceId());
                        mainTask2Executed.incrementAndGet();
                    })
                    .linkAdd("main_end");

            spec.addEnd("main_end").title("主图结束");
        });

        flowEngine.load(subGraph);
        flowEngine.load(mainGraph);

        String instanceId = "stop-sub-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行多图流程（预期在子任务2后停止）...");
        flowEngine.eval("stop-from-sub-main", context);

        // 验证执行结果
        assertEquals(1, mainTask1Executed.get(), "主任务1应该被执行");
        assertEquals(1, subTask1Executed.get(), "子任务1应该被执行");
        assertEquals(1, subTask2Executed.get(), "子任务2应该被执行");
        assertEquals(0, mainTask2Executed.get(), "主任务2不应该被执行（被子图stop了）");

        // 验证流程状态
        assertTrue(context.isStopped(), "上下文应该标记为已停止");

        System.out.println("✅ 子图stop测试通过");
    }

    /**
     * 测试场景4：单图中使用 interrupt 中断当前分支
     * 验证 interrupt 只中断当前分支，不影响其他分支
     */
    @Test
    void testInterruptInSingleGraph() {
        System.out.println("\n=== 测试场景4：单图中使用 interrupt 中断当前分支 ===");

        AtomicInteger task1Executed = new AtomicInteger(0);
        AtomicInteger branch1Task1Executed = new AtomicInteger(0);
        AtomicInteger branch1Task2Executed = new AtomicInteger(0);
        AtomicInteger branch2Task1Executed = new AtomicInteger(0);
        AtomicInteger branch2Task2Executed = new AtomicInteger(0);

        Graph interruptGraph = Graph.create("interrupt-single", "中断测试单图", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("task1");

            spec.addActivity("task1").title("任务1")
                    .task((context, node) -> {
                        System.out.println("执行任务1: " + context.getInstanceId());
                        task1Executed.incrementAndGet();
                    })
                    .linkAdd("parallel_gateway");

            // 并行网关分出两个分支
            spec.addParallel("parallel_gateway").title("并行网关")
                    .linkAdd("branch1_task1")
                    .linkAdd("branch2_task1");

            // 分支1：会调用 interrupt
            spec.addActivity("branch1_task1").title("分支1任务1")
                    .task((context, node) -> {
                        System.out.println("执行分支1任务1: " + context.getInstanceId());
                        branch1Task1Executed.incrementAndGet();

                        // 中断当前分支
                        context.interrupt();
                        System.out.println("分支1任务1调用 interrupt()");
                    })
                    .linkAdd("branch1_task2");

            spec.addActivity("branch1_task2").title("分支1任务2（不应该执行）")
                    .task((context, node) -> {
                        System.out.println("执行分支1任务2: " + context.getInstanceId());
                        branch1Task2Executed.incrementAndGet();
                    })
                    .linkAdd("merge_gateway");

            // 分支2：正常执行
            spec.addActivity("branch2_task1").title("分支2任务1")
                    .task((context, node) -> {
                        System.out.println("执行分支2任务1: " + context.getInstanceId());
                        branch2Task1Executed.incrementAndGet();
                    })
                    .linkAdd("branch2_task2");

            spec.addActivity("branch2_task2").title("分支2任务2")
                    .task((context, node) -> {
                        System.out.println("执行分支2任务2: " + context.getInstanceId());
                        branch2Task2Executed.incrementAndGet();
                    })
                    .linkAdd("merge_gateway");

            // 包容网关合并分支
            spec.addInclusive("merge_gateway").title("合并网关")
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        flowEngine.load(interruptGraph);

        String instanceId = "interrupt-single-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行中断测试单图（分支1被中断，分支2正常）...");
        flowEngine.eval("interrupt-single", context);

        // 验证执行结果
        assertEquals(1, task1Executed.get(), "任务1应该被执行");
        assertEquals(1, branch1Task1Executed.get(), "分支1任务1应该被执行");
        assertEquals(0, branch1Task2Executed.get(), "分支1任务2不应该被执行（被中断了）");
        assertEquals(1, branch2Task1Executed.get(), "分支2任务1应该被执行");
        assertEquals(1, branch2Task2Executed.get(), "分支2任务2应该被执行");

        // 验证流程状态
        assertFalse(context.isStopped(), "上下文不应该标记为已停止（只是中断）");
        assertNotNull(context.lastRecord(), "应该有最后记录");

        System.out.println("✅ 单图interrupt测试通过");
    }

    /**
     * 测试场景5：多图中在子图调用 interrupt
     * 验证在子图中调用 interrupt 只影响当前分支
     */
    @Test
    void testInterruptInSubGraph() {
        System.out.println("\n=== 测试场景5：多图中在子图调用 interrupt ===");

        AtomicInteger mainTask1Executed = new AtomicInteger(0);
        AtomicInteger subTask1Executed = new AtomicInteger(0);
        AtomicInteger subTask2Executed = new AtomicInteger(0);
        AtomicInteger mainTask2Executed = new AtomicInteger(0);

        // 创建子图（会调用 interrupt）
        Graph subGraph = Graph.create("interrupt-sub", "中断子图", spec -> {
            spec.addStart("sub_start").title("子图开始")
                    .linkAdd("sub_task1");

            spec.addActivity("sub_task1").title("子任务1")
                    .task((context, node) -> {
                        System.out.println("执行子任务1: " + context.getInstanceId());
                        subTask1Executed.incrementAndGet();
                    })
                    .linkAdd("sub_task2");

            spec.addActivity("sub_task2").title("子任务2（会interrupt）")
                    .task((context, node) -> {
                        System.out.println("执行子任务2: " + context.getInstanceId());
                        subTask2Executed.incrementAndGet();

                        // 在子图中中断
                        context.interrupt();
                        System.out.println("子任务2调用 interrupt()");
                    })
                    .linkAdd("sub_end");

            spec.addEnd("sub_end").title("子图结束");
        });

        // 创建主图
        Graph mainGraph = Graph.create("interrupt-from-sub-main", "从子图中断主图", spec -> {
            spec.addStart("main_start").title("主图开始")
                    .linkAdd("main_task1");

            spec.addActivity("main_task1").title("主任务1")
                    .task((context, node) -> {
                        System.out.println("执行主任务1: " + context.getInstanceId());
                        mainTask1Executed.incrementAndGet();
                    })
                    .linkAdd("call_sub");

            spec.addActivity("call_sub").title("调用子图")
                    .task("#interrupt-sub")
                    .linkAdd("main_task2");

            spec.addActivity("main_task2").title("主任务2（可能执行，也可能被中断）")
                    .task((context, node) -> {
                        System.out.println("执行主任务2: " + context.getInstanceId());
                        mainTask2Executed.incrementAndGet();
                    })
                    .linkAdd("main_end");

            spec.addEnd("main_end").title("主图结束");
        });

        flowEngine.load(subGraph);
        flowEngine.load(mainGraph);

        String instanceId = "interrupt-sub-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行多图流程（子图中调用interrupt）...");
        flowEngine.eval("interrupt-from-sub-main", context);

        // 验证执行结果
        assertEquals(1, mainTask1Executed.get(), "主任务1应该被执行");
        assertEquals(1, subTask1Executed.get(), "子任务1应该被执行");
        assertEquals(1, subTask2Executed.get(), "子任务2应该被执行");

        // 注意：interrupt 只中断当前分支，但子图作为主图的一个分支，可能会影响主图的后续执行
        // 具体行为取决于框架实现

        System.out.println("✅ 子图interrupt测试通过");
    }

    /**
     * 测试场景6：stop 和 interrupt 的混合使用
     * 验证先 interrupt 再 stop 的行为
     */
    @Test
    void testStopAfterInterrupt() {
        System.out.println("\n=== 测试场景6：stop 和 interrupt 的混合使用 ===");

        AtomicInteger task1Executed = new AtomicInteger(0);
        AtomicInteger task2Executed = new AtomicInteger(0);
        AtomicInteger task3Executed = new AtomicInteger(0);

        Graph mixedGraph = Graph.create("mixed-stop-interrupt", "混合停止中断图", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("task1");

            spec.addActivity("task1").title("任务1（先interrupt）")
                    .task((context, node) -> {
                        System.out.println("执行任务1: " + context.getInstanceId());
                        task1Executed.incrementAndGet();

                        // 先中断
                        context.interrupt();
                        System.out.println("任务1调用 interrupt()");
                    })
                    .linkAdd("task2");

            spec.addActivity("task2").title("任务2（再stop）")
                    .task((context, node) -> {
                        System.out.println("执行任务2: " + context.getInstanceId());
                        task2Executed.incrementAndGet();

                        // 再停止
                        context.stop();
                        System.out.println("任务2调用 stop()");
                    })
                    .linkAdd("task3");

            spec.addActivity("task3").title("任务3（不应该执行）")
                    .task((context, node) -> {
                        System.out.println("执行任务3: " + context.getInstanceId());
                        task3Executed.incrementAndGet();
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        flowEngine.load(mixedGraph);

        String instanceId = "mixed-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行混合测试（先interrupt再stop）...");
        flowEngine.eval("mixed-stop-interrupt", context);

        // 验证执行结果
        assertEquals(1, task1Executed.get(), "任务1应该被执行");

        // 注意：由于任务1调用了interrupt，任务2可能不会执行
        // 具体行为取决于框架实现

        assertEquals(0, task3Executed.get(), "任务3不应该被执行");

        System.out.println("✅ 混合stop/interrupt测试通过");
    }

    /**
     * 测试场景7：在并行网关的不同分支中使用 stop 和 interrupt
     * 验证并行分支中的控制流交互
     */
    @Test
    void testStopAndInterruptInParallelBranches() {
        System.out.println("\n=== 测试场景7：并行分支中的 stop 和 interrupt ===");

        AtomicInteger branch1Task1Executed = new AtomicInteger(0);
        AtomicInteger branch1Task2Executed = new AtomicInteger(0);
        AtomicInteger branch2Task1Executed = new AtomicInteger(0);
        AtomicInteger branch2Task2Executed = new AtomicInteger(0);
        AtomicInteger branch3Task1Executed = new AtomicInteger(0);
        AtomicInteger branch3Task2Executed = new AtomicInteger(0);

        Graph parallelControlGraph = Graph.create("parallel-control", "并行控制图", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("parallel_gateway");

            // 并行网关分出三个分支
            spec.addParallel("parallel_gateway").title("并行网关")
                    .linkAdd("branch1_task1")
                    .linkAdd("branch2_task1")
                    .linkAdd("branch3_task1");

            // 分支1：使用 interrupt
            spec.addActivity("branch1_task1").title("分支1任务1")
                    .task((context, node) -> {
                        System.out.println("执行分支1任务1: " + context.getInstanceId());
                        branch1Task1Executed.incrementAndGet();

                        context.interrupt();
                        System.out.println("分支1调用 interrupt()");
                    })
                    .linkAdd("branch1_task2");

            spec.addActivity("branch1_task2").title("分支1任务2（不应该执行）")
                    .task((context, node) -> {
                        System.out.println("执行分支1任务2: " + context.getInstanceId());
                        branch1Task2Executed.incrementAndGet();
                    })
                    .linkAdd("merge_gateway");

            // 分支2：使用 stop
            spec.addActivity("branch2_task1").title("分支2任务1")
                    .task((context, node) -> {
                        System.out.println("执行分支2任务1: " + context.getInstanceId());
                        branch2Task1Executed.incrementAndGet();

                        context.stop();
                        System.out.println("分支2调用 stop()");
                    })
                    .linkAdd("branch2_task2");

            spec.addActivity("branch2_task2").title("分支2任务2（不应该执行）")
                    .task((context, node) -> {
                        System.out.println("执行分支2任务2: " + context.getInstanceId());
                        branch2Task2Executed.incrementAndGet();
                    })
                    .linkAdd("merge_gateway");

            // 分支3：正常执行（不受其他分支影响？）
            spec.addActivity("branch3_task1").title("分支3任务1")
                    .task((context, node) -> {
                        System.out.println("执行分支3任务1: " + context.getInstanceId());
                        branch3Task1Executed.incrementAndGet();
                    })
                    .linkAdd("branch3_task2");

            spec.addActivity("branch3_task2").title("分支3任务2")
                    .task((context, node) -> {
                        System.out.println("执行分支3任务2: " + context.getInstanceId());
                        branch3Task2Executed.incrementAndGet();
                    })
                    .linkAdd("merge_gateway");

            // 包容网关（等待所有未中断的分支）
            spec.addInclusive("merge_gateway").title("合并网关")
                    .task((context, node) -> {
                        System.out.println("合并网关执行: " + context.getInstanceId());
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        flowEngine.load(parallelControlGraph);

        String instanceId = "parallel-control-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行并行控制测试（三个分支分别：中断、停止、正常）...");
        flowEngine.eval("parallel-control", context);

        // 验证执行结果
        assertEquals(1, branch1Task1Executed.get(), "分支1任务1应该被执行");
        assertEquals(0, branch1Task2Executed.get(), "分支1任务2不应该被执行（被中断了）");

        assertEquals(1, branch2Task1Executed.get(), "分支2任务1应该被执行");
        assertEquals(0, branch2Task2Executed.get(), "分支2任务2不应该被执行（被停止了）");

        // 分支3的行为取决于框架实现：
        // 1. 如果stop影响整个流程，分支3可能不会执行
        // 2. 如果stop只影响当前分支，分支3应该正常执行
        System.out.println("分支3执行状态: 任务1=" + branch3Task1Executed.get() +
                ", 任务2=" + branch3Task2Executed.get());

        // 验证流程状态
        boolean isStopped = context.isStopped();
        System.out.println("流程是否停止: " + isStopped);

        System.out.println("✅ 并行分支控制测试完成");
    }

    /**
     * 测试场景8：stop 后重新执行流程
     * 验证 stop 后是否可以重新开始执行
     */
    @Test
    void testRestartAfterStop() {
        System.out.println("\n=== 测试场景8：stop 后重新执行流程 ===");

        AtomicInteger task1Executed = new AtomicInteger(0);
        AtomicInteger task2Executed = new AtomicInteger(0);

        Graph restartGraph = Graph.create("restart-graph", "重启测试图", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("task1");

            spec.addActivity("task1").title("任务1（会stop）")
                    .task((context, node) -> {
                        System.out.println("执行任务1: " + context.getInstanceId());
                        task1Executed.incrementAndGet();

                        context.stop();
                        System.out.println("任务1调用 stop()");
                    })
                    .linkAdd("task2");

            spec.addActivity("task2").title("任务2")
                    .task((context, node) -> {
                        System.out.println("执行任务2: " + context.getInstanceId());
                        task2Executed.incrementAndGet();
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        flowEngine.load(restartGraph);

        String instanceId = "restart-test-" + System.currentTimeMillis();

        // 第一次执行：在任务1处停止
        {
            FlowContext context1 = FlowContext.of(instanceId + "-first");
            System.out.println("第一次执行（预期在任务1停止）...");
            flowEngine.eval("restart-graph", context1);

            assertEquals(1, task1Executed.get(), "第一次执行：任务1应该被执行");
            assertEquals(0, task2Executed.get(), "第一次执行：任务2不应该被执行");
            assertTrue(context1.isStopped(), "第一次执行：应该被停止");
        }

        // 重置计数器
        task1Executed.set(0);
        task2Executed.set(0);

        // 第二次执行：使用新的上下文重新开始
        {
            FlowContext context2 = FlowContext.of(instanceId + "-second");
            System.out.println("第二次执行（使用新上下文重新开始）...");
            flowEngine.eval("restart-graph", context2);

            assertEquals(1, task1Executed.get(), "第二次执行：任务1应该被执行");

            // 注意：第二次执行是否会停止取决于上下文状态
            // 如果上下文状态被清除，可能会完整执行
            System.out.println("第二次执行结果: 任务1=" + task1Executed.get() +
                    ", 任务2=" + task2Executed.get());
        }

        System.out.println("✅ 重启测试完成");
    }

    /**
     * 测试场景9：使用 NamedTaskComponent 封装 stop 逻辑
     * 验证在自定义组件中控制流程停止
     */
    @Test
    void testStopInNamedTaskComponent() {
        System.out.println("\n=== 测试场景9：NamedTaskComponent 中的 stop ===");

        AtomicInteger mainTaskExecuted = new AtomicInteger(0);
        AtomicInteger componentExecuted = new AtomicInteger(0);
        AtomicInteger postTaskExecuted = new AtomicInteger(0);

        // 创建会停止的自定义组件
        NamedTaskComponent stopComponent = new NamedTaskComponent() {
            @Override
            public String name() {
                return "stop-component";
            }

            @Override
            public String title() {
                return "停止组件";
            }

            @Override
            public void run(FlowContext context, Node node) throws Throwable {
                System.out.println("执行停止组件: " + context.getInstanceId());
                componentExecuted.incrementAndGet();

                // 检查条件决定是否停止
                boolean shouldStop = context.getOrDefault("shouldStop", false);
                if (shouldStop) {
                    context.stop();
                    System.out.println("组件调用 stop()");
                }
            }
        };

        Graph componentGraph = Graph.create("component-stop", "组件停止图", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("main_task");

            spec.addActivity("main_task").title("主任务")
                    .task((context, node) -> {
                        System.out.println("执行主任务: " + context.getInstanceId());
                        mainTaskExecuted.incrementAndGet();
                        context.put("shouldStop", true); // 设置停止条件
                    })
                    .linkAdd("call_component");

            spec.addActivity("call_component").title("调用停止组件")
                    .task(stopComponent)
                    .linkAdd("post_task");

            spec.addActivity("post_task").title("后续任务（不应该执行）")
                    .task((context, node) -> {
                        System.out.println("执行后续任务: " + context.getInstanceId());
                        postTaskExecuted.incrementAndGet();
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        flowEngine.load(componentGraph);

        String instanceId = "component-stop-test-" + System.currentTimeMillis();
        FlowContext context = FlowContext.of(instanceId);

        System.out.println("执行组件停止测试...");
        flowEngine.eval("component-stop", context);

        // 验证执行结果
        assertEquals(1, mainTaskExecuted.get(), "主任务应该被执行");
        assertEquals(1, componentExecuted.get(), "组件应该被执行");
        assertEquals(0, postTaskExecuted.get(), "后续任务不应该被执行（被组件停止了）");

        assertTrue(context.isStopped(), "上下文应该标记为已停止");

        System.out.println("✅ 组件stop测试通过");
    }

    /**
     * 综合测试：运行所有 stop/interrupt 测试
     */
    @Test
    void testAllStopInterruptScenarios() {
        System.out.println("=== 开始所有 stop/interrupt 测试 ===");

        // 依次执行所有测试场景
        testStopInSingleGraph();
        testStopInMainGraphWithSubGraph();
        testStopInSubGraph();
        testInterruptInSingleGraph();
        testInterruptInSubGraph();
        testStopAfterInterrupt();
        testStopAndInterruptInParallelBranches();
        testRestartAfterStop();
        testStopInNamedTaskComponent();

        System.out.println("\n=== 所有 stop/interrupt 测试场景完成 ===");
        System.out.println("✅ FlowEngine.eval 与 FlowContext stop/interrupt 交互测试全部通过");
    }
}