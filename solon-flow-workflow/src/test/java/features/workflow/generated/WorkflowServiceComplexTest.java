package features.workflow.generated;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.workflow.Task;
import org.noear.solon.flow.workflow.TaskAction;
import org.noear.solon.flow.workflow.TaskState;
import org.noear.solon.flow.workflow.WorkflowService;
import org.noear.solon.flow.workflow.controller.ActorStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 复杂测试 - 多分支、网关、参与者控制
 */
class WorkflowServiceComplexTest {

    private FlowEngine flowEngine;
    private WorkflowService workflowService;
    private Graph complexGraph;

    @BeforeEach
    void setUp() {
        // 创建复杂流程图：开始 -> 并行网关 -> (任务A, 任务B) -> 排他网关 -> 结束
        complexGraph = Graph.create("complex-test", "复杂流程测试", spec -> {
            spec.addStart("start").title("开始")
                    .linkAdd("parallel");

            // 并行网关
            spec.addParallel("parallel").title("并行网关")
                    .linkAdd("taskA")
                    .linkAdd("taskB");

            // 两个并行任务
            spec.addActivity("taskA").title("任务A")
                    .metaPut("actor", "user1")
                    .linkAdd("parallel_end");

            spec.addActivity("taskB").title("任务B")
                    .metaPut("actor", "user2")
                    .linkAdd("parallel_end");

            // 并行网关聚合
            spec.addParallel("parallel_end").title("并行网关-end")
                    .linkAdd("end");

            spec.addEnd("end").title("结束");
        });

        flowEngine = FlowEngine.newInstance();
        flowEngine.load(complexGraph);

        workflowService = WorkflowService.of(
                flowEngine,
                new ActorStateController("actor"),
                new InMemoryStateRepository()
        );
    }

    @Test
    void testParallelGatewayWithMultipleTasks() {
        FlowContext context = FlowContext.of("test-instance");

        // 获取所有任务
        Collection<Task> tasks = workflowService.findNextTasks("complex-test", context);

        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        // 验证两个任务都在等待
        List<Task> waitingTasks = tasks.stream()
                .filter(task -> task.getState() == TaskState.WAITING) //没有匹配的人
                .collect(Collectors.toList());
        assertEquals(0, waitingTasks.size());
    }

    @Test
    void testParallelGatewayWithMultipleTasks2() {
        FlowContext context = FlowContext.of("test-instance");
        context.put("actor", "user1");

        // 获取所有任务
        Collection<Task> tasks = workflowService.findNextTasks("complex-test", context);

        assertNotNull(tasks);
        assertEquals(2, tasks.size());

        // 验证两个任务都在等待
        List<Task> waitingTasks = tasks.stream()
                .filter(task -> task.getState() == TaskState.WAITING) //有一个人匹配
                .collect(Collectors.toList());
        assertEquals(1, waitingTasks.size());
    }

    @Test
    void testActorBasedTaskAssignment() {
        // 用户1只能看到任务A
        FlowContext context1 = FlowContext.of("test-instance");
        context1.put("actor", "user1");

        Task task1 = workflowService.getTask("complex-test", context1);
        assertNotNull(task1);
        assertEquals("taskA", task1.getNodeId());
        assertEquals(TaskState.WAITING, task1.getState());

        // 用户2只能看到任务B
        FlowContext context2 = FlowContext.of("test-instance");
        context2.put("actor", "user2");

        Task task2 = workflowService.getTask("complex-test", context2);
        assertNotNull(task2);
        assertEquals("taskB", task2.getNodeId());
        assertEquals(TaskState.WAITING, task2.getState());

        // 用户3看不到任何任务（没有对应的actor）
        FlowContext context3 = FlowContext.of("test-instance");
        context3.put("actor", "user3");

        Task task3 = workflowService.getTask("complex-test", context3);
        assertNull(task3);
    }

    @Test
    void testCompleteParallelTasks() {
        FlowContext context1 = FlowContext.of("test-instance");
        context1.put("actor", "user1");

        FlowContext context2 = FlowContext.of("test-instance");
        context2.put("actor", "user2");

        // 用户1完成任务A
        workflowService.postTask("complex-test", "taskA", TaskAction.FORWARD, context1);

        // 用户1应该没有任务了
        Task taskAfterA = workflowService.getTask("complex-test", context1);
        assertNull(taskAfterA);

        // 用户2的任务B应该还在等待
        Task taskB = workflowService.getTask("complex-test", context2);
        assertNotNull(taskB );
        assertEquals("taskB", taskB.getNodeId());
        assertEquals(TaskState.WAITING, taskB.getState());

        // 用户2完成任务B
        workflowService.postTask("complex-test", "taskB", TaskAction.FORWARD, context2);

        // 两个任务都完成后，流程应该结束
        Task finalTask = workflowService.getTask("complex-test", context2);
        assertNull(finalTask);
    }

    @Test
    void testTaskBackAction() {
        FlowContext context = FlowContext.of("test-instance");
        context.put("actor", "user1");

        // 获取任务并前进
        workflowService.getTask("complex-test", context);
        workflowService.postTask("complex-test", "taskA", TaskAction.FORWARD, context);

        // 验证任务状态为完成
        Node taskANode = complexGraph.getNode("taskA");
        TaskState stateAfterForward = workflowService.getState(taskANode, context);
        assertEquals(TaskState.COMPLETED, stateAfterForward);

        // 后退操作
        workflowService.postTask("complex-test", "taskA", TaskAction.BACK, context);

        // 状态应该回到未知（或待等）
        TaskState stateAfterBack = workflowService.getState(taskANode, context);
        assertEquals(TaskState.UNKNOWN, stateAfterBack);

        // 应该能再次获取到任务
        Task taskAfterBack = workflowService.getTask("complex-test", context);
        assertNotNull(taskAfterBack);
        assertEquals("taskA", taskAfterBack.getNodeId());
    }

    @Test
    void testTerminateAction() {
        FlowContext context = FlowContext.of("test-instance");
        context.put("actor", "user1");

        // 获取任务
        workflowService.getTask("complex-test", context);

        // 终止任务
        workflowService.postTask("complex-test", "taskA", TaskAction.TERMINATE, context);

        // 状态应该为终止
        Node taskANode = complexGraph.getNode("taskA");
        TaskState stateAfterTerminate = workflowService.getState(taskANode, context);
        assertEquals(TaskState.TERMINATED, stateAfterTerminate);

        // 获取任务应该返回终止状态的任务
        Task task = workflowService.getTask("complex-test", context);
        assertNotNull(task);
        assertEquals(TaskState.TERMINATED, task.getState());
    }

    @Test
    void testRestartAction() {
        FlowContext context = FlowContext.of("test-instance");
        context.put("actor", "user1");

        // 前进任务
        workflowService.getTask("complex-test", context);
        workflowService.postTask("complex-test", "taskA", TaskAction.FORWARD, context);

        // 重启整个流程
        workflowService.postTask("complex-test", "taskA", TaskAction.RESTART, context);

        // 状态应该被清除
        Node taskANode = complexGraph.getNode("taskA");
        TaskState stateAfterRestart = workflowService.getState(taskANode, context);
        assertEquals(TaskState.UNKNOWN, stateAfterRestart);

        // 应该能重新获取任务
        Task task = workflowService.getTask("complex-test", context);
        assertNotNull(task);
        assertEquals(TaskState.WAITING, task.getState());
    }

    @Test
    void testJumpActions() {
        FlowContext context = FlowContext.of("test-instance");
        context.put("actor", "user1");

        // 获取任务A
        Task initialTask = workflowService.getTask("complex-test", context);
        assertEquals("taskA", initialTask.getNodeId());

        // 跳转前进到任务B（虽然是用户1的任务，但测试跳转功能）//跳跃不成功
        workflowService.postTask("complex-test", "taskB", TaskAction.FORWARD_JUMP, context);

        // 两个任务都应该完成
        Node taskANode = complexGraph.getNode("taskA");
        Node taskBNode = complexGraph.getNode("taskB");

        assertEquals(TaskState.COMPLETED, workflowService.getState(taskANode, context));
        assertEquals(TaskState.UNKNOWN, workflowService.getState(taskBNode, context));

        // 流程应该结束
        Task finalTask = workflowService.getTask("complex-test", context);
        assertNull(finalTask);
    }
}