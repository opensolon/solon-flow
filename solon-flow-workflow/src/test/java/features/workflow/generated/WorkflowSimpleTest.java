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
 * 简单测试 - 基础功能验证
 */
class WorkflowSimpleTest {

    private FlowEngine flowEngine;
    private WorkflowExecutor workflowService;
    private Graph simpleGraph;

    @BeforeEach
    void setUp() {
        // 创建简单流程图
        simpleGraph = Graph.create("simple-test", spec -> {
            spec.addStart("start").title("开始").linkAdd("task1");
            spec.addActivity("task1").title("任务1").linkAdd("end");
            spec.addEnd("end").title("结束");
        });

        flowEngine = FlowEngine.newInstance();
        flowEngine.load(simpleGraph);

        workflowService = WorkflowExecutor.of(
                flowEngine,
                new BlockStateController(),
                new InMemoryStateRepository()
        );
    }

    @Test
    void testWorkflowServiceCreation() {
        assertNotNull(workflowService);
        assertNotNull(workflowService.engine());
        assertEquals(flowEngine, workflowService.engine());
    }

    @Test
    void testGetTaskOnNewInstance() {
        FlowContext context = FlowContext.of("test-instance");

        Task task = workflowService.findTask("simple-test", context);

        assertNotNull(task);
        assertEquals("task1", task.getNodeId());
        assertEquals(TaskState.WAITING, task.getState());
    }

    @Test
    void testFindNextTasksReturnsCollection() {
        FlowContext context = FlowContext.of("test-instance");

        Collection<Task> tasks = workflowService.findNextTasks("simple-test", context);

        assertNotNull(tasks);
        assertFalse(tasks.isEmpty());
        assertEquals(1, tasks.size());

        Task task = tasks.iterator().next();
        assertEquals("task1", task.getNodeId());
    }

    @Test
    void testPostTaskForward() {
        FlowContext context = FlowContext.of("test-instance");

        // 初始状态应该是等待
        Task initialTask = workflowService.findTask("simple-test", context);
        assertEquals(TaskState.WAITING, initialTask.getState());

        // 提交前进操作
        workflowService.submitTask("simple-test", "task1", TaskAction.FORWARD, context);

        // 流程应该已完成，没有等待的任务
        Task taskAfterForward = workflowService.findTask("simple-test", context);
        assertNull(taskAfterForward);
    }

    @Test
    void testGetState() {
        FlowContext context = FlowContext.of("test-instance");
        Node taskNode = simpleGraph.getNode("task1");

        TaskState initialState = workflowService.getState(taskNode, context);
        assertEquals(TaskState.UNKNOWN, initialState);

        // 获取任务后会设置状态为等待
        workflowService.findTask("simple-test", context);

        TaskState stateAfterGet = workflowService.getState(taskNode, context);
        assertEquals(TaskState.WAITING, stateAfterGet);
    }

    @Test
    void testClearState() {
        FlowContext context = FlowContext.of("test-instance");

        // 先获取任务，设置状态
        workflowService.findTask("simple-test", context);

        // 清除状态
        workflowService.stateRepository().stateClear(context);

        // 再次获取应该重新开始
        Task task = workflowService.findTask("simple-test", context);
        assertNotNull(task);
        assertEquals(TaskState.WAITING, task.getState());
    }

    @Test
    void testPostTaskIfWaiting() {
        FlowContext context = FlowContext.of("test-instance");

        // 初始应该可以提交
        Task task = workflowService.findTask("simple-test", context);
        boolean result1 = workflowService.submitTaskIfWaiting(task, TaskAction.FORWARD, context
        );
        assertTrue(result1);

        // 再次尝试应该失败，因为状态已不是等待
        task = workflowService.findTask("simple-test", context);
        boolean result2 = workflowService.submitTaskIfWaiting(task, TaskAction.FORWARD, context
        );
        assertFalse(result2);
    }

    @Test
    void testTaskRunMethod() throws Exception {
        FlowContext context = FlowContext.of("test-instance");
        context.put("testValue", "initial");

        Task task = workflowService.findTask("simple-test", context);
        assertNotNull(task);

        // 测试运行任务（虽然简单图没有实际任务，但应该不会抛出异常）
        assertDoesNotThrow(() -> task.run(context));
    }
}