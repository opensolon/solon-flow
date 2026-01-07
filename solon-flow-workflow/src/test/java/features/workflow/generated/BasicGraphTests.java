package features.workflow.generated;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.workflow.Task;
import org.noear.solon.flow.workflow.TaskAction;
import org.noear.solon.flow.workflow.TaskState;
import org.noear.solon.flow.workflow.WorkflowExecutor;
import org.noear.solon.flow.workflow.controller.BlockStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础图结构测试
 * 测试简单的线性图、分支图、并行图等基础结构
 */
class BasicGraphTests {
    private FlowEngine flowEngine;
    private WorkflowExecutor workflow;

    @BeforeEach
    void setUp() {
        flowEngine = FlowEngine.newInstance();
        workflow = WorkflowExecutor.of(
                flowEngine,
                new BlockStateController(),
                new InMemoryStateRepository()
        );
    }

    @Test
    void testLinearGraphFindNextTasks() {
        // 线性图: start -> A -> B -> C -> end
        Graph graph = Graph.create("linear-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("A");
            spec.addActivity("A").title("任务A").linkAdd("B");
            spec.addActivity("B").title("任务B").linkAdd("C");
            spec.addActivity("C").title("任务C").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);
        FlowContext context = FlowContext.of("test-1");

        // 初始状态，下一个任务应该是A
        Collection<Task> tasks = workflow.findNextTasks("linear-test", context);
        assertEquals(1, tasks.size());
        assertEquals("A", tasks.iterator().next().getNodeId());
        assertEquals(TaskState.WAITING, tasks.iterator().next().getState());

        // 完成A后，下一个应该是B
        workflow.submitTask("linear-test", "A", TaskAction.FORWARD, context);
        tasks = workflow.findNextTasks("linear-test", context);
        assertEquals(1, tasks.size());
        assertEquals("B", tasks.iterator().next().getNodeId());
    }

    @Test
    void testBranchGraphWithExclusiveGateway() {
        // 排他网关图: start -> gateway -> (A -> end) / (B -> end)
        Graph graph = Graph.create("branch-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("gateway");
            spec.addExclusive("gateway").title("选择网关")
                    .linkAdd("A", l -> l.when("x > 10"))
                    .linkAdd("B", l -> l.when("x <= 10"));
            spec.addActivity("A").title("任务A").linkAdd("end");
            spec.addActivity("B").title("任务B").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);

        // 测试分支A
        FlowContext context1 = FlowContext.of("branch-test-1");
        context1.put("x", 15);
        Collection<Task> tasks1 = workflow.findNextTasks("branch-test", context1);
        assertEquals(1, tasks1.size());
        assertEquals("A", tasks1.iterator().next().getNodeId());

        // 测试分支B
        FlowContext context2 = FlowContext.of("branch-test-2");
        context2.put("x", 5);
        Collection<Task> tasks2 = workflow.findNextTasks("branch-test", context2);
        assertEquals(1, tasks2.size());
        assertEquals("B", tasks2.iterator().next().getNodeId());
    }

    @Test
    void testParallelGraphFindNextTasks() {
        // 并行网关图: start -> gateway -> (A, B) -> merge -> end
        Graph graph = Graph.create("parallel-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("split");
            spec.addParallel("split").title("并行拆分").linkAdd("A").linkAdd("B");
            spec.addActivity("A").title("任务A").linkAdd("merge");
            spec.addActivity("B").title("任务B").linkAdd("merge");
            spec.addParallel("merge").title("并行合并").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);
        FlowContext context = FlowContext.of("parallel-test");

        // 并行网关后，应该找到两个任务
        Collection<Task> tasks = workflow.findNextTasks("parallel-test", context);
        assertEquals(2, tasks.size());

        // 验证包含A和B
        boolean hasA = tasks.stream().anyMatch(t -> "A".equals(t.getNodeId()));
        boolean hasB = tasks.stream().anyMatch(t -> "B".equals(t.getNodeId()));
        assertTrue(hasA);
        assertTrue(hasB);
    }

    @Test
    void testComplexNestedGraph() {
        // 复杂嵌套图: 包含多个网关嵌套
        Graph graph = Graph.create("complex-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("gateway1");

            // 第一层排他网关
            spec.addExclusive("gateway1").title("第一层选择")
                    .linkAdd("path1", l -> l.when("choice == 1"))
                    .linkAdd("gateway2", l -> l.when("choice == 2"))
                    .linkAdd("end", l -> l.when("choice == 3"));

            // 路径1: 简单路径
            spec.addActivity("path1").title("路径1任务").linkAdd("end");

            // 路径2: 包含并行网关
            spec.addParallel("gateway2").title("并行拆分")
                    .linkAdd("taskA").linkAdd("taskB");
            spec.addActivity("taskA").title("并行任务A").linkAdd("gateway3");
            spec.addActivity("taskB").title("并行任务B").linkAdd("gateway3");
            spec.addParallel("gateway3").title("并行合并").linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);

        // 测试不同路径的选择
        FlowContext context1 = FlowContext.of("complex-1");
        context1.put("choice", 1);
        Collection<Task> tasks1 = workflow.findNextTasks("complex-test", context1);
        assertEquals(1, tasks1.size());
        assertEquals("path1", tasks1.iterator().next().getNodeId());

        FlowContext context2 = FlowContext.of("complex-2");
        context2.put("choice", 2);
        Collection<Task> tasks2 = workflow.findNextTasks("complex-test", context2);
        assertEquals(2, tasks2.size()); // 并行网关产生两个任务
    }
}