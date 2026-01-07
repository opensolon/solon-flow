package features.workflow.generated;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.*;
import org.noear.solon.flow.workflow.Task;
import org.noear.solon.flow.workflow.TaskAction;
import org.noear.solon.flow.workflow.TaskState;
import org.noear.solon.flow.workflow.WorkflowExecutor;
import org.noear.solon.flow.workflow.controller.ActorStateController;
import org.noear.solon.flow.workflow.controller.BlockStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowExecutor方法组合测试
 * 测试findNextTasks, matchTask, findTask的交互和影响
 */
class ExecutorMethodTests {
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
    void testFindNextTasksVsMatchTask() {
        // 创建包含并行网关的图
        Graph graph = Graph.create("compare-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("split");
            spec.addParallel("split").title("并行拆分").linkAdd("A").linkAdd("B");
            spec.addActivity("A").title("任务A").metaPut("actor", "user1").linkAdd("merge");
            spec.addActivity("B").title("任务B").metaPut("actor", "user2").linkAdd("merge");
            spec.addParallel("merge").title("并行合并").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);

        // 测试findNextTasks - 应该返回所有可能的下一个任务
        FlowContext context = FlowContext.of("compare-1");
        context.put("actor", "user1");

        Collection<Task> nextTasks = workflowExecutor.findNextTasks("compare-test", context);
        assertEquals(2, nextTasks.size()); // 应该包含A和B

        // 测试matchTask - 应该只返回当前用户可处理的任务
        Task matchTask = workflowExecutor.claimTask("compare-test", context);
        assertNotNull(matchTask);
        assertEquals("A", matchTask.getNodeId()); // 只匹配user1的任务

        // 验证状态
        assertEquals(TaskState.WAITING, matchTask.getState());
    }

    @Test
    void testFindTaskVsMatchTask() {
        // 创建线性图
        Graph graph = Graph.create("find-vs-match", spec -> {
            spec.addStart("start").title("开始").linkAdd("A");
            spec.addActivity("A").title("任务A").metaPut("actor", "user1").linkAdd("B");
            spec.addActivity("B").title("任务B").metaPut("actor", "user2").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);
        workflowExecutor = WorkflowExecutor.of(flowEngine, new ActorStateController(), new InMemoryStateRepository());

        FlowContext contextUser1 = FlowContext.of("user1-context");
        contextUser1.put("actor", "user1");

        FlowContext contextUser2 = FlowContext.of("user2-context");
        contextUser2.put("actor", "user2");

        // user1的matchTask应该返回A
        Task taskUser1 = workflowExecutor.claimTask("find-vs-match", contextUser1);
        assertNotNull(taskUser1);
        assertEquals("A", taskUser1.getNodeId());

        // user1的findTask也应该返回A
        Task findTaskUser1 = workflowExecutor.findTask("find-vs-match", contextUser1);
        assertNotNull(findTaskUser1);
        assertEquals("A", findTaskUser1.getNodeId());

        // user2的matchTask应该返回null（因为A还没完成）
        Task taskUser2 = workflowExecutor.claimTask("find-vs-match", contextUser2);
        assertNull(taskUser2);

        // user2的findTask应该返回A（逻辑上A是当前任务）
        Task findTaskUser2 = workflowExecutor.findTask("find-vs-match", contextUser2);
        assertNotNull(findTaskUser2);
        assertEquals("A", findTaskUser2.getNodeId());
        assertEquals(TaskState.UNKNOWN, findTaskUser2.getState());
    }

    @Test
    void testFindNextTasksWithDifferentStates() {
        Graph graph = Graph.create("state-variation", spec -> {
            spec.addStart("start").title("开始").linkAdd("gateway");
            spec.addExclusive("gateway").title("选择网关")
                    .linkAdd("A", l -> l.when("\"open\".equals(status)"))
                    .linkAdd("B", l -> l.when("\"closed\".equals(status)"))
                    .linkAdd("C"); // 默认路径
            spec.addActivity("A").title("开放任务").linkAdd("end");
            spec.addActivity("B").title("关闭任务").linkAdd("end");
            spec.addActivity("C").title("默认任务").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);

        // 测试不同状态下的findNextTasks
        FlowContext context1 = FlowContext.of("state-1");
        context1.put("status", "open");
        Collection<Task> tasks1 = workflowExecutor.findNextTasks("state-variation", context1);
        assertEquals(1, tasks1.size());
        assertEquals("A", tasks1.iterator().next().getNodeId());

        FlowContext context2 = FlowContext.of("state-2");
        context2.put("status", "closed");
        Collection<Task> tasks2 = workflowExecutor.findNextTasks("state-variation", context2);
        assertEquals(1, tasks2.size());
        assertEquals("B", tasks2.iterator().next().getNodeId());

        FlowContext context3 = FlowContext.of("state-3");
        context3.put("status", "");
        // 不设置status，应该走默认路径
        Collection<Task> tasks3 = workflowExecutor.findNextTasks("state-variation", context3);
        assertEquals(1, tasks3.size());
        assertEquals("C", tasks3.iterator().next().getNodeId());
    }

    @Test
    void testMethodInteractionsAfterActions() {
        Graph graph = Graph.create("interaction-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("A");
            spec.addActivity("A").title("任务A").linkAdd("B");
            spec.addActivity("B").title("任务B").linkAdd("C");
            spec.addActivity("C").title("任务C").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);
        FlowContext context = FlowContext.of("interaction-context");

        // 初始状态测试
        Collection<Task> initialTasks = workflowExecutor.findNextTasks("interaction-test", context);
        assertEquals(1, initialTasks.size());
        assertEquals("A", initialTasks.iterator().next().getNodeId());

        Task matchTask1 = workflowExecutor.claimTask("interaction-test", context);
        assertEquals("A", matchTask1.getNodeId());

        Task findTask1 = workflowExecutor.findTask("interaction-test", context);
        assertEquals("A", findTask1.getNodeId());

        // 执行FORWARD动作
        workflowExecutor.submitTask("interaction-test", "A", TaskAction.FORWARD, context);

        // 动作后测试
        Collection<Task> tasksAfterForward = workflowExecutor.findNextTasks("interaction-test", context);
        assertEquals(1, tasksAfterForward.size());
        assertEquals("B", tasksAfterForward.iterator().next().getNodeId());

        Task matchTask2 = workflowExecutor.claimTask("interaction-test", context);
        assertEquals("B", matchTask2.getNodeId());

        Task findTask2 = workflowExecutor.findTask("interaction-test", context);
        assertEquals("B", findTask2.getNodeId());

        // 执行BACK动作
        workflowExecutor.submitTask("interaction-test", "A", TaskAction.BACK, context);

        // 回退后测试
        Collection<Task> tasksAfterBack = workflowExecutor.findNextTasks("interaction-test", context);
        assertEquals(1, tasksAfterBack.size());
        assertEquals("A", tasksAfterBack.iterator().next().getNodeId());

        Task matchTask3 = workflowExecutor.claimTask("interaction-test", context);
        assertEquals("A", matchTask3.getNodeId());

        Task findTask3 = workflowExecutor.findTask("interaction-test", context);
        assertEquals("A", findTask3.getNodeId());
    }

    @Test
    void testFindNextTasksWithMultiplePaths() {
        // 创建具有多个可能路径的图
        Graph graph = Graph.create("multi-path", spec -> {
            spec.addStart("start").title("开始").linkAdd("gateway1");

            spec.addInclusive("gateway1").title("包容网关")
                    .linkAdd("A", l -> l.when("option1"))
                    .linkAdd("B", l -> l.when("option2"))
                    .linkAdd("C", l -> l.when("option3"));

            spec.addActivity("A").title("选项A").linkAdd("end");
            spec.addActivity("B").title("选项B").linkAdd("end");
            spec.addActivity("C").title("选项C").linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);

        // 测试多个条件同时满足的情况
        FlowContext context = FlowContext.of("multi-path-context");
        context.put("option1", true);
        context.put("option2", true);
        context.put("option3", false);

        Collection<Task> tasks = workflowExecutor.findNextTasks("multi-path", context);
        assertEquals(2, tasks.size()); // A和B

        List<String> taskIds = tasks.stream()
                .map(Task::getNodeId)
                .sorted()
                .collect(Collectors.toList());

        assertEquals(Arrays.asList("A", "B"), taskIds);
    }

    @Test
    void testMatchTaskWithActorController() {
        // 使用ActorStateController测试权限匹配
        WorkflowExecutor actorWorkflow = WorkflowExecutor.of(
                flowEngine,
                new ActorStateController("role", "department"),
                new InMemoryStateRepository()
        );

        Graph graph = Graph.create("actor-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("A");
            spec.addActivity("A").title("管理员任务")
                    .metaPut("role", "admin")
                    .linkAdd("B");
            spec.addActivity("B").title("部门任务")
                    .metaPut("department", "sales")
                    .linkAdd("C");
            spec.addActivity("C").title("公共任务").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(graph);

        // 测试管理员权限
        FlowContext adminContext = FlowContext.of("1");
        adminContext.put("role", "admin");
        Task adminTask = actorWorkflow.claimTask("actor-test", adminContext);
        assertNotNull(adminTask);
        assertEquals("A", adminTask.getNodeId());

        // 测试部门权限（A还没完成，不能访问B）
        FlowContext salesContext = FlowContext.of("1");
        salesContext.put("department", "sales");

        Task salesTask = actorWorkflow.claimTask("actor-test", salesContext);
        assertNull(salesTask); // 应该为null，因为A还没完成

        // 先完成A
        actorWorkflow.submitTask("actor-test", "A", TaskAction.FORWARD, adminContext);

        // 现在销售部门可以访问B了
        Task salesTaskAfter = actorWorkflow.claimTask("actor-test", salesContext);
        assertNotNull(salesTaskAfter);
        assertEquals("B", salesTaskAfter.getNodeId());

        // 测试无权限用户
        FlowContext noRoleContext = FlowContext.of("no-role-context");
        Task noRoleTask = actorWorkflow.claimTask("actor-test", noRoleContext);
        assertNull(noRoleTask);
    }
}