package features.workflow.generated;

import org.noear.solon.flow.FlowContext;
import org.noear.solon.flow.FlowEngine;
import org.noear.solon.flow.Graph;
import org.noear.solon.flow.Node;
import org.noear.solon.flow.NodeType;
import org.noear.solon.flow.workflow.*;
import org.noear.solon.flow.workflow.controller.ActorStateController;
import org.noear.solon.flow.workflow.repository.InMemoryStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkflowService 多图多角色审批测试用例
 * 场景描述：
 * 1. 多图嵌套：主审批图 + 两个子图（技术评审图、财务评审图）
 * 2. 多角色参与：申请人、部门经理、技术负责人、财务负责人、总经理
 * 3. 复杂流程：并行审批、条件分支、子图调用
 */
public class WorkflowServiceMultiGraphMultiRoleTest {

    private FlowEngine flowEngine;
    private WorkflowService workflowService;

    // 角色定义
    private static final String ROLE_APPLICANT = "applicant";
    private static final String ROLE_DEPT_MANAGER = "deptManager";
    private static final String ROLE_TECH_LEADER = "techLeader";
    private static final String ROLE_FINANCE_LEADER = "financeLeader";
    private static final String ROLE_GENERAL_MANAGER = "generalManager";

    // 图ID定义
    private static final String MAIN_APPROVAL_GRAPH_ID = "mainApproval";
    private static final String TECH_REVIEW_GRAPH_ID = "techReview";
    private static final String FINANCE_REVIEW_GRAPH_ID = "financeReview";

    @BeforeEach
    void setUp() {
        // 1. 创建流程引擎
        flowEngine = FlowEngine.newInstance();

        // 2. 构建子图1：技术评审图（并行评审）
        Graph techReviewGraph = Graph.create(TECH_REVIEW_GRAPH_ID, "技术评审流程", spec -> {
            spec.addStart("tech_start").title("技术评审开始")
                    .linkAdd("tech_review1");

            // 技术方案评审
            spec.addActivity("tech_review1").title("技术方案评审")
                    .metaPut("actor", ROLE_TECH_LEADER)
                    .metaPut("department", "技术部")
                    .linkAdd("tech_parallel");

            // 并行网关：同时进行代码评审和架构评审
            spec.addParallel("tech_parallel").title("并行技术评审")
                    .linkAdd("code_review")
                    .linkAdd("arch_review");

            // 代码评审
            spec.addActivity("code_review").title("代码评审")
                    .metaPut("actor", ROLE_TECH_LEADER)
                    .metaPut("reviewType", "code")
                    .linkAdd("tech_merge");

            // 架构评审
            spec.addActivity("arch_review").title("架构评审")
                    .metaPut("actor", ROLE_TECH_LEADER)
                    .metaPut("reviewType", "architecture")
                    .linkAdd("tech_merge");

            // 包容网关：合并并行分支
            spec.addInclusive("tech_merge").title("技术评审合并")
                    .linkAdd("tech_end");

            spec.addEnd("tech_end").title("技术评审结束");
        });

        // 3. 构建子图2：财务评审图（条件分支）
        Graph financeReviewGraph = Graph.create(FINANCE_REVIEW_GRAPH_ID, "财务评审流程", spec -> {
            spec.addStart("finance_start").title("财务评审开始")
                    .linkAdd("budget_review");

            // 预算评审
            spec.addActivity("budget_review").title("预算评审")
                    .metaPut("actor", ROLE_FINANCE_LEADER)
                    .metaPut("amount", "budget")
                    .linkAdd("finance_decision");

            // 排他网关：根据金额决定是否需要额外审批
            spec.addExclusive("finance_decision").title("财务决策")
                    .linkAdd("normal_approval", link -> link
                            .when("amount <= 100000")
                            .title("常规审批"))
                    .linkAdd("extra_approval", link -> link
                            .when("amount > 100000")
                            .title("额外审批")
                            .priority(10))
                    .linkAdd("normal_approval"); // 默认分支

            // 常规审批路径
            spec.addActivity("normal_approval").title("财务常规审批")
                    .metaPut("actor", ROLE_FINANCE_LEADER)
                    .linkAdd("finance_end");

            // 额外审批路径（金额较大时需要）
            spec.addActivity("extra_approval").title("财务额外审批")
                    .metaPut("actor", ROLE_FINANCE_LEADER)
                    .metaPut("requireGeneralManager", true)
                    .linkAdd("finance_end");

            spec.addEnd("finance_end").title("财务评审结束");
        });

        // 4. 构建主审批图（调用子图）
        Graph mainApprovalGraph = Graph.create(MAIN_APPROVAL_GRAPH_ID, "主审批流程", spec -> {
            // 开始节点
            spec.addStart("start").title("流程开始")
                    .linkAdd("apply");

            // 申请提交
            spec.addActivity("apply").title("提交申请")
                    .metaPut("actor", ROLE_APPLICANT)
                    .task((context, node) -> {
                        // 模拟申请提交逻辑
                        context.put("applicantName", "张三");
                        context.put("projectName", "新项目开发");
                        context.put("budgetAmount", 150000); // 15万，触发额外财务审批
                        context.put("hasTechnicalReview", true);
                    })
                    .linkAdd("dept_approval");

            // 部门经理审批
            spec.addActivity("dept_approval").title("部门经理审批")
                    .metaPut("actor", ROLE_DEPT_MANAGER)
                    .task((context, node) -> {
                        // 模拟部门经理审批逻辑
                        boolean approved = Math.random() > 0.1; // 90%通过率
                        context.put("deptApproved", approved);
                        context.put("deptComment", approved ? "同意" : "需要修改");
                    })
                    .linkAdd("parallel_gateway");

            // 并行网关：同时进行技术评审和财务评审
            spec.addParallel("parallel_gateway").title("并行评审网关")
                    .linkAdd("tech_review_call")
                    .linkAdd("finance_review_call");

            // 调用技术评审子图
            spec.addActivity("tech_review_call").title("技术评审")
                    .task("#" + TECH_REVIEW_GRAPH_ID) // 调用子图
                    .linkAdd("merge_gateway");

            // 调用财务评审子图
            spec.addActivity("finance_review_call").title("财务评审")
                    .task("#" + FINANCE_REVIEW_GRAPH_ID) // 调用子图
                    .linkAdd("merge_gateway");

            // 包容网关：合并技术评审和财务评审结果
            spec.addInclusive("merge_gateway").title("评审结果合并")
                    .linkAdd("final_decision");

            // 最终决策（排他网关）
            spec.addExclusive("final_decision").title("最终决策")
                    .linkAdd("general_manager_approval", link -> link
                            .when("budgetAmount > 100000 || requireExtraApproval == true")
                            .title("需要总经理审批")
                            .priority(10))
                    .linkAdd("complete_approval", link -> link
                            .when("deptApproved == true && techReviewPassed == true && financeReviewPassed == true")
                            .title("直接完成审批"))
                    .linkAdd("reject"); // 默认拒绝

            // 总经理审批（大额或特殊审批）
            spec.addActivity("general_manager_approval").title("总经理审批")
                    .metaPut("actor", ROLE_GENERAL_MANAGER)
                    .metaPut("level", "final")
                    .task((context, node) -> {
                        boolean approved = Math.random() > 0.05; // 95%通过率
                        context.put("generalManagerApproved", approved);
                        context.put("finalComment", approved ? "最终同意" : "最终拒绝");
                    })
                    .linkAdd("complete_approval", link -> link.when("generalManagerApproved == true"))
                    .linkAdd("reject");

            // 完成审批
            spec.addActivity("complete_approval").title("审批完成")
                    .task((context, node) -> {
                        // 模拟审批完成逻辑
                        context.put("approvalStatus", "APPROVED");
                        context.put("completionTime", System.currentTimeMillis());
                    })
                    .linkAdd("end");

            // 拒绝
            spec.addActivity("reject").title("审批拒绝")
                    .task((context, node) -> {
                        context.put("approvalStatus", "REJECTED");
                        context.put("rejectReason", "不符合公司政策");
                    })
                    .linkAdd("end");

            spec.addEnd("end").title("流程结束");
        });

        // 5. 加载所有图到引擎
        flowEngine.load(techReviewGraph);
        flowEngine.load(financeReviewGraph);
        flowEngine.load(mainApprovalGraph);

        // 6. 创建工作流服务
        workflowService = WorkflowService.of(
                flowEngine,
                new ActorStateController("actor"), // 基于角色的状态控制器
                new InMemoryStateRepository()     // 内存状态仓库
        );
    }

    @Test
    void testMultiGraphMultiRoleApprovalProcess() {
        // 测试场景：完整的多图多角色审批流程

        // 1. 申请人提交申请
        FlowContext applicantContext = FlowContext.of("test_instance_001")
                .put("actor", ROLE_APPLICANT)
                .put("applicantId", "user_001");

        // 获取申请人的当前任务
        Task applicantTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, applicantContext);
        assertNotNull(applicantTask);
        assertEquals("apply", applicantTask.getNodeId());
        assertEquals(TaskState.WAITING, applicantTask.getState());

        // 申请人执行任务
        workflowService.postTask(applicantTask.getNode(), TaskAction.FORWARD, applicantContext);

        // 2. 部门经理审批
        FlowContext deptManagerContext = FlowContext.of("test_instance_001")
                .put("actor", ROLE_DEPT_MANAGER)
                .put("managerId", "manager_001");

        Task deptManagerTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, deptManagerContext);
        assertNotNull(deptManagerTask);
        assertEquals("dept_approval", deptManagerTask.getNodeId());

        // 部门经理审批通过
        workflowService.postTask(deptManagerTask.getNode(), TaskAction.FORWARD, deptManagerContext);

        // 3. 验证进入并行网关，获取并行任务
        FlowContext systemContext = FlowContext.of("test_instance_001");
        Collection<Task> parallelTasks = workflowService.getNextTasks(MAIN_APPROVAL_GRAPH_ID, systemContext);
        assertEquals(2, parallelTasks.size()); // 应该有技术评审和财务评审两个任务

        // 4. 技术负责人进行技术评审
        FlowContext techLeaderContext = FlowContext.of("test_instance_001")
                .put("actor", ROLE_TECH_LEADER)
                .put("techLeaderId", "tech_001");

        // 获取技术评审子图中的任务
        Collection<Task> techReviewTasks = workflowService.getNextTasks(TECH_REVIEW_GRAPH_ID, techLeaderContext);
        assertFalse(techReviewTasks.isEmpty());

        // 技术负责人完成技术评审
        for (Task task : techReviewTasks) {
            workflowService.postTask(task.getNode(), TaskAction.FORWARD, techLeaderContext);
        }

        // 5. 财务负责人进行财务评审
        FlowContext financeLeaderContext = FlowContext.of("test_instance_001")
                .put("actor", ROLE_FINANCE_LEADER)
                .put("financeLeaderId", "finance_001")
                .put("amount", 150000); // 设置金额触发额外审批

        // 获取财务评审子图中的任务
        Collection<Task> financeReviewTasks = workflowService.getNextTasks(FINANCE_REVIEW_GRAPH_ID, financeLeaderContext);

        // 财务负责人完成财务评审（由于金额较大，会触发额外审批路径）
        for (Task task : financeReviewTasks) {
            workflowService.postTask(task.getNode(), TaskAction.FORWARD, financeLeaderContext);
        }

        // 6. 验证需要总经理审批
        FlowContext checkContext = FlowContext.of("test_instance_001");
        Task nextTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, checkContext);

        // 由于金额较大，应该需要总经理审批
        if (nextTask != null && "general_manager_approval".equals(nextTask.getNodeId())) {
            // 7. 总经理审批
            FlowContext generalManagerContext = FlowContext.of("test_instance_001")
                    .put("actor", ROLE_GENERAL_MANAGER)
                    .put("generalManagerId", "gm_001");

            Task gmTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, generalManagerContext);
            assertNotNull(gmTask);
            assertEquals("general_manager_approval", gmTask.getNodeId());

            // 总经理审批通过
            workflowService.postTask(gmTask.getNode(), TaskAction.FORWARD, generalManagerContext);
        }

        // 8. 验证流程完成
        Task finalTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, systemContext);
        if (finalTask != null) {
            assertEquals("complete_approval", finalTask.getNodeId());
            workflowService.postTask(finalTask.getNode(), TaskAction.FORWARD, systemContext);
        }

        // 9. 验证状态
        Node endNode = flowEngine.getGraph(MAIN_APPROVAL_GRAPH_ID).getNode("end");
        TaskState finalState = workflowService.getState(endNode, systemContext);
        assertNotNull(finalState);
    }

    @Test
    void testParallelApprovalWithMultipleActors() {
        // 测试并行审批场景：多个角色同时参与

        FlowContext context = FlowContext.of("test_instance_002")
                .put("applicantName", "李四")
                .put("projectName", "并行测试项目")
                .put("budgetAmount", 50000);

        // 1. 申请人提交
        context.put("actor", ROLE_APPLICANT);
        Task applicantTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowService.postTask(applicantTask.getNode(), TaskAction.FORWARD, context);

        // 2. 部门经理审批
        context.put("actor", ROLE_DEPT_MANAGER);
        Task deptTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowService.postTask(deptTask.getNode(), TaskAction.FORWARD, context);

        // 3. 模拟技术负责人和财务负责人并行审批
        FlowContext techContext = FlowContext.of("test_instance_002")
                .put("actor", ROLE_TECH_LEADER);

        FlowContext financeContext = FlowContext.of("test_instance_002")
                .put("actor", ROLE_FINANCE_LEADER)
                .put("amount", 50000);

        // 获取技术评审任务
        Collection<Task> techTasks = workflowService.getNextTasks(TECH_REVIEW_GRAPH_ID, techContext);

        // 获取财务评审任务
        Collection<Task> financeTasks = workflowService.getNextTasks(FINANCE_REVIEW_GRAPH_ID, financeContext);

        // 并行执行技术评审
        for (Task task : techTasks) {
            workflowService.postTask(task.getNode(), TaskAction.FORWARD, techContext);
        }

        // 并行执行财务评审
        for (Task task : financeTasks) {
            workflowService.postTask(task.getNode(), TaskAction.FORWARD, financeContext);
        }

        // 验证主流程继续
        FlowContext checkContext = FlowContext.of("test_instance_002");
        Task nextTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, checkContext);
        assertNotNull(nextTask);
    }

    @Test
    void testApprovalRejectionAndBacktrack() {
        // 测试审批拒绝和回退场景

        FlowContext context = FlowContext.of("test_instance_003");

        // 1. 提交申请
        context.put("actor", ROLE_APPLICANT);
        Task applicantTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowService.postTask(applicantTask.getNode(), TaskAction.FORWARD, context);

        // 2. 部门经理拒绝
        context.put("actor", ROLE_DEPT_MANAGER);
        Task deptTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowService.postTask(deptTask.getNode(), TaskAction.TERMINATE, context); // 终止审批

        // 3. 验证流程终止
        FlowContext checkContext = FlowContext.of("test_instance_003");
        Task finalTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, checkContext);

        // 应该进入拒绝节点
        assertNotNull(finalTask);
        assertEquals("reject", finalTask.getNodeId());
    }

    @Test
    void testJumpApproval() {
        // 测试跳转审批场景

        FlowContext context = FlowContext.of("test_instance_004");

        // 提交申请
        context.put("actor", ROLE_APPLICANT);
        Task applicantTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowService.postTask(applicantTask.getNode(), TaskAction.FORWARD, context);

        // 部门经理审批
        context.put("actor", ROLE_DEPT_MANAGER);
        Task deptTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowService.postTask(deptTask.getNode(), TaskAction.FORWARD, context);

        // 模拟总经理直接跳转到最终审批（跳过技术评审和财务评审）
        context.put("actor", ROLE_GENERAL_MANAGER);

        // 获取当前任务
        Task currentTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context);

        if (currentTask != null) {
            // 跳转到最终审批节点
            Node finalApprovalNode = flowEngine.getGraph(MAIN_APPROVAL_GRAPH_ID).getNode("general_manager_approval");
            workflowService.postTask(finalApprovalNode, TaskAction.FORWARD_JUMP, context);
        }

        // 验证状态
        FlowContext checkContext = FlowContext.of("test_instance_004");
        Task finalTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, checkContext);
        assertNotNull(finalTask);
    }

    @Test
    void testMultiInstanceWorkflow() {
        // 测试多实例工作流同时运行

        for (int i = 1; i <= 3; i++) {
            String instanceId = "multi_instance_" + i;
            FlowContext context = FlowContext.of(instanceId)
                    .put("actor", ROLE_APPLICANT)
                    .put("applicantName", "用户" + i)
                    .put("projectName", "项目" + i)
                    .put("budgetAmount", i * 50000);

            // 启动每个实例
            Task task = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context);
            assertNotNull(task);
            workflowService.postTask(task.getNode(), TaskAction.FORWARD, context);

            // 验证每个实例独立运行
            FlowContext checkContext = FlowContext.of(instanceId);
            Task currentTask = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, checkContext);
            assertNotNull(currentTask);
            assertEquals(TaskState.WAITING, currentTask.getState());
        }
    }

    @Test
    void testWorkflowWithConditionalBranches() {
        // 测试条件分支工作流

        // 场景1：小金额项目，不需要总经理审批
        testSmallAmountProject();

        // 场景2：大金额项目，需要总经理审批
        testLargeAmountProject();
    }

    private void testSmallAmountProject() {
        FlowContext context = FlowContext.of("small_amount_instance")
                .put("budgetAmount", 50000); // 5万，小金额

        // 提交申请
        context.put("actor", ROLE_APPLICANT);
        Task task = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowService.postTask(task.getNode(), TaskAction.FORWARD, context);

        // 部门经理审批
        context.put("actor", ROLE_DEPT_MANAGER);
        task = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowService.postTask(task.getNode(), TaskAction.FORWARD, context);

        // 验证不会进入总经理审批节点
        FlowContext checkContext = FlowContext.of("small_amount_instance");
        Collection<Task> tasks = workflowService.getNextTasks(MAIN_APPROVAL_GRAPH_ID, checkContext);

        boolean hasGeneralManagerTask = tasks.stream()
                .anyMatch(t -> "general_manager_approval".equals(t.getNodeId()));
        assertFalse(hasGeneralManagerTask, "小金额项目不应需要总经理审批");
    }

    private void testLargeAmountProject() {
        FlowContext context = FlowContext.of("large_amount_instance")
                .put("budgetAmount", 200000); // 20万，大金额

        // 提交申请
        context.put("actor", ROLE_APPLICANT);
        Task task = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowService.postTask(task.getNode(), TaskAction.FORWARD, context);

        // 部门经理审批
        context.put("actor", ROLE_DEPT_MANAGER);
        task = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context);
        workflowService.postTask(task.getNode(), TaskAction.FORWARD, context);

        // 验证会进入总经理审批节点
        FlowContext checkContext = FlowContext.of("large_amount_instance");
        Collection<Task> tasks = workflowService.getNextTasks(MAIN_APPROVAL_GRAPH_ID, checkContext);

        boolean hasGeneralManagerTask = tasks.stream()
                .anyMatch(t -> "general_manager_approval".equals(t.getNodeId()));
        assertTrue(hasGeneralManagerTask, "大金额项目应需要总经理审批");
    }

    @Test
    void testWorkflowStatePersistence() {
        // 测试工作流状态持久化和恢复

        String instanceId = "persistence_test_instance";

        // 第一阶段：提交申请
        FlowContext context1 = FlowContext.of(instanceId)
                .put("actor", ROLE_APPLICANT);

        Task task1 = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context1);
        workflowService.postTask(task1.getNode(), TaskAction.FORWARD, context1);

        // 保存当前状态（模拟持久化）
        String stateJson = context1.toJson();

        // 第二阶段：从持久化状态恢复
        FlowContext context2 = FlowContext.fromJson(stateJson);
        context2.put("actor", ROLE_DEPT_MANAGER);

        Task task2 = workflowService.getTask(MAIN_APPROVAL_GRAPH_ID, context2);
        assertNotNull(task2);
        assertEquals("dept_approval", task2.getNodeId());

        // 继续执行
        workflowService.postTask(task2.getNode(), TaskAction.FORWARD, context2);

        // 验证流程继续
        FlowContext checkContext = FlowContext.of(instanceId);
        Collection<Task> tasks = workflowService.getNextTasks(MAIN_APPROVAL_GRAPH_ID, checkContext);
        assertFalse(tasks.isEmpty());
    }
}