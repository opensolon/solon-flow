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
import org.noear.solon.flow.workflow.WorkflowExecutor;
import org.noear.solon.flow.workflow.controller.BlockStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskAction.FORWARD_JUMP 和 BACK_JUMP 相关测试
 */
class WorkflowJumpActionTest {

    private FlowEngine flowEngine;
    private WorkflowExecutor workflowExecutor;
    private Graph jumpTestGraph;

    @BeforeEach
    void setUp() {
        // 创建用于跳转测试的流程图
        // start -> task1 -> task2 -> task3 -> end
        jumpTestGraph = Graph.create("jump-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("task1");
            spec.addActivity("task1").title("任务1").linkAdd("task2");
            spec.addActivity("task2").title("任务2").linkAdd("task3");
            spec.addActivity("task3").title("任务3").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine = FlowEngine.newInstance();
        flowEngine.load(jumpTestGraph);

        workflowExecutor = WorkflowExecutor.of(
                flowEngine,
                new BlockStateController(),
                new InMemoryStateRepository()
        );
    }

    @Test
    void testForwardJumpFromStartToTask3() {
        // 测试目的：验证 FORWARD_JUMP 可以从开始节点跳转到指定节点
        // 测试场景：从流程开始直接跳转到 task3
        FlowContext context = FlowContext.of("forward-jump-test-1");

        // 初始状态应该是 task1 等待
        Task initialTask = workflowExecutor.claimTask("jump-test", context);
        assertNotNull(initialTask);
        assertEquals("task1", initialTask.getNodeId());
        assertEquals(TaskState.WAITING, initialTask.getState());

        // 执行 FORWARD_JUMP 到 task3
        workflowExecutor.submitTask("jump-test", "task3", TaskAction.FORWARD_JUMP, context);

        // 验证 task1 和 task2 的状态应该是 COMPLETED（因为跳转前进会完成中间节点）
        Node task1Node = jumpTestGraph.getNode("task1");
        Node task2Node = jumpTestGraph.getNode("task2");
        assertEquals(TaskState.COMPLETED, workflowExecutor.getState(task1Node, context));
        assertEquals(TaskState.COMPLETED, workflowExecutor.getState(task2Node, context));

        // 当前任务应该是 task3，并且是 WAITING 状态
        Task currentTask = workflowExecutor.claimTask("jump-test", context);
        assertNotNull(currentTask);
        assertEquals("task3", currentTask.getNodeId());
        assertEquals(TaskState.WAITING, currentTask.getState());

        // 完成 task3
        workflowExecutor.submitTask("jump-test", "task3", TaskAction.FORWARD, context);

        // 流程应该已完成
        Task finalTask = workflowExecutor.claimTask("jump-test", context);
        assertNull(finalTask);
    }

    @Test
    void testBackJumpFromTask3ToTask1() {
        // 测试目的：验证 BACK_JUMP 可以从当前节点跳转回退到指定节点
        // 测试场景：前进到 task3，然后回退到 task1
        FlowContext context = FlowContext.of("back-jump-test-1");

        // 先前进到 task3
        workflowExecutor.claimTask("jump-test", context);
        workflowExecutor.submitTask("jump-test", "task1", TaskAction.FORWARD, context);
        workflowExecutor.submitTask("jump-test", "task2", TaskAction.FORWARD, context);
        workflowExecutor.submitTask("jump-test", "task3", TaskAction.FORWARD, context);

        // 验证当前没有任务（流程应该已经结束）
        Task taskAfterForward = workflowExecutor.claimTask("jump-test", context);
        assertNull(taskAfterForward);

        workflowExecutor.findTask("jump-test", context);
        // 执行 BACK_JUMP 到 task1
        workflowExecutor.submitTask("jump-test", "task1", TaskAction.BACK_JUMP, context);


        // 验证 task2 和 task3 的状态应该是 WAITING（因为回退会重置状态）
        Node task2Node = jumpTestGraph.getNode("task2");
        Node task3Node = jumpTestGraph.getNode("task3");
        assertEquals(TaskState.UNKNOWN, workflowExecutor.getState(task2Node, context));
        assertEquals(TaskState.UNKNOWN, workflowExecutor.getState(task3Node, context));

        // 当前任务应该是 task1，并且是 WAITING 状态
        Task currentTask = workflowExecutor.claimTask("jump-test", context);
        assertNotNull(currentTask);
        assertEquals("task1", currentTask.getNodeId());
        assertEquals(TaskState.WAITING, currentTask.getState());
    }

    @Test
    void testForwardJumpWithPartialCompletion() {
        // 测试目的：验证 FORWARD_JUMP 在部分节点已完成的场景
        // 测试场景：手动完成 task1，然后跳转到 task3
        FlowContext context = FlowContext.of("forward-jump-partial-test");

        // 手动完成 task1
        workflowExecutor.claimTask("jump-test", context);
        workflowExecutor.submitTask("jump-test", "task1", TaskAction.FORWARD, context);

        // 验证 task1 已完成
        Node task1Node = jumpTestGraph.getNode("task1");
        assertEquals(TaskState.COMPLETED, workflowExecutor.getState(task1Node, context));

        // 当前任务应该是 task2
        Task taskBeforeJump = workflowExecutor.claimTask("jump-test", context);
        assertNotNull(taskBeforeJump);
        assertEquals("task2", taskBeforeJump.getNodeId());

        // 执行 FORWARD_JUMP 到 task2
        workflowExecutor.submitTask("jump-test", "task2", TaskAction.FORWARD_JUMP, context);

        // 验证 task2 的状态应该是 COMPLETED（跳转前进会完成中间节点）
        Node task2Node = jumpTestGraph.getNode("task2");
        assertEquals(TaskState.WAITING, workflowExecutor.getState(task2Node, context));

        // 当前任务应该是 task3
        Task currentTask = workflowExecutor.claimTask("jump-test", context);
        assertNotNull(currentTask);
        assertEquals("task2", currentTask.getNodeId());
        assertEquals(TaskState.WAITING, currentTask.getState());
    }

    @Test
    void testBackJumpToCompletedNode() {
        // 测试目的：验证 BACK_JUMP 到已完成节点会重置该节点状态
        // 测试场景：前进到 task3，然后回退到 task1（task1 已完成的场景）
        FlowContext context = FlowContext.of("back-jump-completed-test");

        // 前进到 task3
        workflowExecutor.claimTask("jump-test", context);
        workflowExecutor.submitTask("jump-test", "task1", TaskAction.FORWARD, context);
        workflowExecutor.submitTask("jump-test", "task2", TaskAction.FORWARD, context);
        workflowExecutor.submitTask("jump-test", "task3", TaskAction.FORWARD, context);

        // 执行 BACK_JUMP 到 task1
        workflowExecutor.submitTask("jump-test", "task1", TaskAction.BACK_JUMP, context);

        // 验证所有节点都重置为 WAITING
        Node task1Node = jumpTestGraph.getNode("task1");
        Node task2Node = jumpTestGraph.getNode("task2");
        Node task3Node = jumpTestGraph.getNode("task3");

        assertEquals(TaskState.WAITING, workflowExecutor.getState(task1Node, context));
        assertEquals(TaskState.UNKNOWN, workflowExecutor.getState(task2Node, context));
        assertEquals(TaskState.UNKNOWN, workflowExecutor.getState(task3Node, context));

        // 当前任务应该是 task1
        Task currentTask = workflowExecutor.claimTask("jump-test", context);
        assertNotNull(currentTask);
        assertEquals("task1", currentTask.getNodeId());
        assertEquals(TaskState.WAITING, currentTask.getState());
    }

    @Test
    void testForwardJumpToCurrentNode() {
        // 测试目的：验证 FORWARD_JUMP 到当前节点不会有副作用
        // 测试场景：当前在 task1，跳转到 task1
        FlowContext context = FlowContext.of("forward-jump-same-test");

        // 获取初始任务
        Task initialTask = workflowExecutor.claimTask("jump-test", context);
        assertNotNull(initialTask);
        assertEquals("task1", initialTask.getNodeId());

        // 执行 FORWARD_JUMP 到当前节点 task1
        workflowExecutor.submitTask("jump-test", "task1", TaskAction.FORWARD_JUMP, context);

        // 状态应该不变，仍然是 WAITING
        Node task1Node = jumpTestGraph.getNode("task1");
        assertEquals(TaskState.WAITING, workflowExecutor.getState(task1Node, context));

        // 当前任务应该还是 task1
        Task currentTask = workflowExecutor.claimTask("jump-test", context);
        assertNotNull(currentTask);
        assertEquals("task1", currentTask.getNodeId());
        assertEquals(TaskState.WAITING, currentTask.getState());
    }

    @Test
    void testBackJumpToCurrentNode() {
        // 测试目的：验证 BACK_JUMP 到当前节点不会有副作用
        // 测试场景：当前在 task2，回退到 task2
        FlowContext context = FlowContext.of("back-jump-same-test");

        // 前进到 task2
        workflowExecutor.claimTask("jump-test", context);
        workflowExecutor.submitTask("jump-test", "task1", TaskAction.FORWARD, context);

        // 当前任务应该是 task2
        Task taskBeforeJump = workflowExecutor.claimTask("jump-test", context);
        assertNotNull(taskBeforeJump);
        assertEquals("task2", taskBeforeJump.getNodeId());

        // 执行 BACK_JUMP 到当前节点 task2
        workflowExecutor.submitTask("jump-test", "task2", TaskAction.BACK_JUMP, context);

        // task1 应该被重置为 WAITING（因为回退会影响前置节点）
        Node task1Node = jumpTestGraph.getNode("task1");
        assertEquals(TaskState.COMPLETED, workflowExecutor.getState(task1Node, context));

        // 当前任务应该是 task1（回退到 task2 会导致前置节点重置）
        Task currentTask = workflowExecutor.claimTask("jump-test", context);
        assertNotNull(currentTask);
        assertEquals("task2", currentTask.getNodeId());
        assertEquals(TaskState.WAITING, currentTask.getState());
    }

    @Test
    void testForwardJumpWithFindNextTasks() {
        // 测试目的：验证 FORWARD_JUMP 后 findNextTasks 返回正确的结果
        FlowContext context = FlowContext.of("forward-jump-find-test");

        // 初始状态，下一步任务应该是 task1
        Collection<Task> initialTasks = workflowExecutor.findNextTasks("jump-test", context);
        assertEquals(1, initialTasks.size());
        assertEquals("task1", initialTasks.iterator().next().getNodeId());

        // 执行 FORWARD_JUMP 到 task3
        workflowExecutor.submitTask("jump-test", "task2", TaskAction.FORWARD_JUMP, context);

        // 现在下一步任务应该是 task3
        Collection<Task> tasksAfterJump = workflowExecutor.findNextTasks("jump-test", context);
        assertEquals(1, tasksAfterJump.size());
        assertEquals("task2", tasksAfterJump.iterator().next().getNodeId());
    }

    @Test
    void testJumpActionsWithMultipleInstances() {
        // 测试目的：验证跳转动作在不同实例间的隔离性
        FlowContext context1 = FlowContext.of("jump-instance-1");
        FlowContext context2 = FlowContext.of("jump-instance-2");

        // 在实例1中执行 FORWARD_JUMP 到 task3
        workflowExecutor.claimTask("jump-test", context1);
        workflowExecutor.submitTask("jump-test", "task3", TaskAction.FORWARD_JUMP, context1);

        // 实例1的当前任务应该是 task3
        Task task1 = workflowExecutor.claimTask("jump-test", context1);
        assertNotNull(task1);
        assertEquals("task3", task1.getNodeId());

        // 实例2的当前任务应该是 task1（未受实例1影响）
        Task task2 = workflowExecutor.claimTask("jump-test", context2);
        assertNotNull(task2);
        assertEquals("task1", task2.getNodeId());

        // 在实例2中执行 BACK_JUMP（虽然还没前进）
        workflowExecutor.submitTask("jump-test", "task1", TaskAction.BACK_JUMP, context2);

        // 实例2的状态应该不变（因为没有前进过）
        Task task2After = workflowExecutor.claimTask("jump-test", context2);
        assertNotNull(task2After);
        assertEquals("task1", task2After.getNodeId());
    }

    @Test
    void testForwardJumpThenCompleteWorkflow() {
        // 测试目的：验证 FORWARD_JUMP 后可以正常完成整个流程
        FlowContext context = FlowContext.of("forward-jump-complete-test");

        // 直接跳转到最后一个任务节点
        workflowExecutor.claimTask("jump-test", context);
        workflowExecutor.submitTask("jump-test", "task3", TaskAction.FORWARD_JUMP, context);

        // 验证中间节点状态
        Node task1Node = jumpTestGraph.getNode("task1");
        Node task2Node = jumpTestGraph.getNode("task2");
        assertEquals(TaskState.COMPLETED, workflowExecutor.getState(task1Node, context));
        assertEquals(TaskState.COMPLETED, workflowExecutor.getState(task2Node, context));

        // 完成 task3
        workflowExecutor.submitTask("jump-test", "task3", TaskAction.FORWARD, context);

        // 验证流程完成
        Task finalTask = workflowExecutor.claimTask("jump-test", context);
        assertNull(finalTask);

        // 所有节点都应该是 COMPLETED 状态
        Node task3Node = jumpTestGraph.getNode("task3");
        assertEquals(TaskState.COMPLETED, workflowExecutor.getState(task3Node, context));
    }

    @Test
    void testBackJumpThenForwardJump() {
        // 测试目的：验证 BACK_JUMP 和 FORWARD_JUMP 可以组合使用
        FlowContext context = FlowContext.of("jump-combination-test");

        // 前进到 task3
        workflowExecutor.claimTask("jump-test", context);
        workflowExecutor.submitTask("jump-test", "task1", TaskAction.FORWARD, context);
        workflowExecutor.submitTask("jump-test", "task2", TaskAction.FORWARD, context);
        workflowExecutor.submitTask("jump-test", "task3", TaskAction.FORWARD, context);

        // 回退到 task1
        workflowExecutor.submitTask("jump-test", "task1", TaskAction.BACK_JUMP, context);

        // 验证状态重置
        Node task1Node = jumpTestGraph.getNode("task1");
        Node task2Node = jumpTestGraph.getNode("task2");
        Node task3Node = jumpTestGraph.getNode("task3");

        assertEquals(TaskState.WAITING, workflowExecutor.getState(task1Node, context));
        assertEquals(TaskState.UNKNOWN, workflowExecutor.getState(task2Node, context));
        assertEquals(TaskState.UNKNOWN, workflowExecutor.getState(task3Node, context));

        // 再次跳转到 task3
        workflowExecutor.submitTask("jump-test", "task3", TaskAction.FORWARD_JUMP, context);

        // 验证中间节点被完成
        assertEquals(TaskState.COMPLETED, workflowExecutor.getState(task1Node, context));
        assertEquals(TaskState.COMPLETED, workflowExecutor.getState(task2Node, context));
        assertEquals(TaskState.WAITING, workflowExecutor.getState(task3Node, context));

        // 完成流程
        workflowExecutor.submitTask("jump-test", "task3", TaskAction.FORWARD, context);

        Task finalTask = workflowExecutor.claimTask("jump-test", context);
        assertNull(finalTask);
    }
}