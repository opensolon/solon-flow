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

import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskAction全面测试
 * 测试所有TaskAction枚举值的影响
 */
class TaskActionTests {
    private FlowEngine flowEngine;
    private WorkflowExecutor workflow;
    private Graph testGraph;

    @BeforeEach
    void setUp() {
        flowEngine = FlowEngine.newInstance();

        // 创建测试图: start -> A -> B -> C -> end
        testGraph = Graph.create("action-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("A");
            spec.addActivity("A").title("任务A").linkAdd("B");
            spec.addActivity("B").title("任务B").linkAdd("C");
            spec.addActivity("C").title("任务C").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine.load(testGraph);

        workflow = WorkflowExecutor.of(
                flowEngine,
                new BlockStateController(),
                new InMemoryStateRepository()
        );
    }

    @Test
    void testForwardAction() {
        FlowContext context = FlowContext.of("forward-test");

        // 初始状态
        Task initialTask = workflow.claimTask("action-test", context);
        assertNotNull(initialTask);
        assertEquals("A", initialTask.getNodeId());
        assertEquals(TaskState.WAITING, initialTask.getState());

        // 执行FORWARD动作
        workflow.submitTask("action-test", "A", TaskAction.FORWARD, context);

        // 验证状态变化
        Node nodeA = testGraph.getNode("A");
        assertEquals(TaskState.COMPLETED, workflow.getState(nodeA, context));

        // 下一个任务应该是B
        Task nextTask = workflow.claimTask("action-test", context);
        assertNotNull(nextTask);
        assertEquals("B", nextTask.getNodeId());
        assertEquals(TaskState.WAITING, nextTask.getState());
    }

    @Test
    void testBackAction() {
        FlowContext context = FlowContext.of("back-test");

        // 前进到B
        workflow.claimTask("action-test", context);
        workflow.submitTask("action-test", "A", TaskAction.FORWARD, context);
        workflow.submitTask("action-test", "B", TaskAction.FORWARD, context);

        // 当前应该在C
        Task taskAtC = workflow.claimTask("action-test", context);
        assertEquals("C", taskAtC.getNodeId());

        // 执行BACK动作回到B
        workflow.submitTask("action-test", "B", TaskAction.BACK, context);

        // B的状态应该是WAITING
        Node nodeB = testGraph.getNode("B");
        assertEquals(TaskState.UNKNOWN, workflow.getState(nodeB, context));

        // C的状态应该是UNKNOWN
        Node nodeC = testGraph.getNode("C");
        assertEquals(TaskState.WAITING, workflow.getState(nodeC, context));

        // 当前任务应该是B
        Task currentTask = workflow.claimTask("action-test", context);
        assertEquals("A", currentTask.getNodeId());
    }

    @Test
    void testTerminateAction() {
        FlowContext context = FlowContext.of("terminate-test");

        // 前进到B
        workflow.claimTask("action-test", context);
        workflow.submitTask("action-test", "A", TaskAction.FORWARD, context);

        // 终止B
        workflow.submitTask("action-test", "B", TaskAction.TERMINATE, context);

        // B的状态应该是TERMINATED
        Node nodeB = testGraph.getNode("B");
        assertEquals(TaskState.TERMINATED, workflow.getState(nodeB, context));

        // 流程应该停止，没有下一个任务
        Task nextTask = workflow.claimTask("action-test", context);
        assertNull(nextTask);

        // findTask应该能找到终止的任务
        Task foundTask = workflow.findTask("action-test", context);
        assertNotNull(foundTask);
        assertEquals("B", foundTask.getNodeId());
        assertEquals(TaskState.TERMINATED, foundTask.getState());
    }

    @Test
    void testRestartAction() {
        FlowContext context = FlowContext.of("restart-test");

        // 前进到C
        workflow.claimTask("action-test", context);
        workflow.submitTask("action-test", "A", TaskAction.FORWARD, context);
        workflow.submitTask("action-test", "B", TaskAction.FORWARD, context);
        workflow.submitTask("action-test", "C", TaskAction.FORWARD, context);

        // 验证所有节点都完成
        assertEquals(TaskState.COMPLETED, workflow.getState(testGraph.getNode("A"), context));
        assertEquals(TaskState.COMPLETED, workflow.getState(testGraph.getNode("B"), context));
        assertEquals(TaskState.COMPLETED, workflow.getState(testGraph.getNode("C"), context));

        // 执行RESTART动作
        workflow.submitTask("action-test", "A", TaskAction.RESTART, context);

        // 所有状态应该被清空
        assertEquals(TaskState.UNKNOWN, workflow.getState(testGraph.getNode("A"), context));
        assertEquals(TaskState.UNKNOWN, workflow.getState(testGraph.getNode("B"), context));
        assertEquals(TaskState.UNKNOWN, workflow.getState(testGraph.getNode("C"), context));

        // 流程应该从A重新开始
        Task restartTask = workflow.claimTask("action-test", context);
        assertEquals("A", restartTask.getNodeId());
        assertEquals(TaskState.WAITING, restartTask.getState());
    }

    @Test
    void testForwardJumpAction() {
        FlowContext context = FlowContext.of("forward-jump-test");

        // 直接从A跳转到C
        workflow.claimTask("action-test", context);
        workflow.submitTask("action-test", "C", TaskAction.FORWARD_JUMP, context);

        // A和B应该自动完成
        assertEquals(TaskState.COMPLETED, workflow.getState(testGraph.getNode("A"), context));
        assertEquals(TaskState.COMPLETED, workflow.getState(testGraph.getNode("B"), context));

        // 当前任务应该是C
        Task currentTask = workflow.claimTask("action-test", context);
        assertEquals("C", currentTask.getNodeId());
        assertEquals(TaskState.WAITING, currentTask.getState());
    }

    @Test
    void testBackJumpAction() {
        FlowContext context = FlowContext.of("back-jump-test");

        // 前进到C
        workflow.claimTask("action-test", context);
        workflow.submitTask("action-test", "A", TaskAction.FORWARD, context);
        workflow.submitTask("action-test", "B", TaskAction.FORWARD, context);
        workflow.submitTask("action-test", "C", TaskAction.FORWARD, context);

        // 从C跳转回退到A
        workflow.submitTask("action-test", "A", TaskAction.BACK_JUMP, context);

        // B和C应该被重置
        assertEquals(TaskState.UNKNOWN, workflow.getState(testGraph.getNode("B"), context));
        assertEquals(TaskState.UNKNOWN, workflow.getState(testGraph.getNode("C"), context));

        // 当前任务应该是A
        Task currentTask = workflow.claimTask("action-test", context);
        assertEquals("A", currentTask.getNodeId());
        assertEquals(TaskState.WAITING, currentTask.getState());
    }

    @Test
    void testSubmitTaskIfWaiting() {
        FlowContext context = FlowContext.of("submit-if-waiting-test");

        // 获取当前任务
        Task task = workflow.claimTask("action-test", context);
        assertNotNull(task);
        assertEquals(TaskState.WAITING, task.getState());
        assertEquals("A", task.getNodeId());

        // 使用submitTaskIfWaiting提交
        boolean result = workflow.submitTaskIfWaiting(task, TaskAction.FORWARD, context);
        assertTrue(result);

        // 验证任务已完成
        assertEquals(TaskState.COMPLETED, workflow.getState(testGraph.getNode("A"), context));

        // 再次尝试提交应该失败
        Task sameTask = workflow.claimTask("action-test", context);
        assertEquals("B", sameTask.getNodeId());
        boolean result2 = workflow.submitTaskIfWaiting(sameTask, TaskAction.FORWARD, context);
        assertTrue(result2);
    }

    @Test
    void testUnknownAction() {
        FlowContext context = FlowContext.of("unknown-action-test");

        // 尝试使用UNKNOWN动作应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            workflow.submitTask("action-test", "A", TaskAction.UNKNOWN, context);
        });
    }

    @Test
    void testActionSequence() {
        FlowContext context = FlowContext.of("action-sequence-test");

        // 测试一系列动作组合
        workflow.claimTask("action-test", context);

        // 前进A
        workflow.submitTask("action-test", "A", TaskAction.FORWARD, context);
        assertEquals(TaskState.COMPLETED, workflow.getState(testGraph.getNode("A"), context));

        // 前进B
        workflow.submitTask("action-test", "B", TaskAction.FORWARD, context);
        assertEquals(TaskState.COMPLETED, workflow.getState(testGraph.getNode("B"), context));

        // 回退到A
        workflow.submitTask("action-test", "A", TaskAction.BACK, context);
        assertEquals(TaskState.UNKNOWN, workflow.getState(testGraph.getNode("A"), context));
        assertEquals(TaskState.COMPLETED, workflow.getState(testGraph.getNode("B"), context));

        // 再次前进A
        workflow.submitTask("action-test", "A", TaskAction.FORWARD, context);
        assertEquals(TaskState.COMPLETED, workflow.getState(testGraph.getNode("A"), context));

        // 跳转到C
        workflow.submitTask("action-test", "C", TaskAction.FORWARD_JUMP, context);
        assertEquals(TaskState.COMPLETED, workflow.getState(testGraph.getNode("B"), context));
        assertEquals(TaskState.WAITING, workflow.getState(testGraph.getNode("C"), context));

        // 完成流程
        workflow.submitTask("action-test", "C", TaskAction.FORWARD, context);
        Task finalTask = workflow.claimTask("action-test", context);
        assertNull(finalTask);
    }
}